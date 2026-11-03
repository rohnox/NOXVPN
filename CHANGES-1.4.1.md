# 1.4.1 — OpenVPN/Cisco entry hang investigation

The user reports that opening the protocol hub closes the app. No Android exception/ANR trace was available, so the exact device failure has not been reproduced or proven resolved.

Code inspection found a definite startup deadlock path introduced by the integration: the host binds the separate OpenVPN status service, whose pipe writer waits for `VpnStatus.readFileLog`. The original OpenVPN Application initializes that process's log cache through its local status binding. The embedded host excludes that Application and did not initialize the service's cache. Meanwhile the client drained this pipe synchronously inside `ServiceConnection.onServiceConnected` on the UI thread. Consequently opening the hub could freeze indefinitely and lead to ANR/termination.

Changes:

- Initialize OpenVPN's existing asynchronous log cache in the status-service process before registering listeners/serving the pipe; make initialization idempotent across service recreation.
- Move the status handshake/Binder reads and pipe drain to an app-owned worker. Cancel/close the previous reader on reconnect/disconnect. A stalled service must not block the UI callback.
- Set the listener context before binding, avoiding an immediate-callback race.
- Read framed packet lengths as unsigned, reject empty/truncated frames safely and close the pipe on completion/failure/cancellation. Recoverable IPC diagnostics record exception types only, without log contents or configuration data.
- Increment version to 1.4.1 / universal version code 5076000, keeping the same release certificate and native inputs.

Regression tests exercise a service opener that never responds, replacement/cancellation, asynchronous open failure, a 40000-byte frame, terminator/closure and corrupt/truncated frames. See VALIDATION.md for actual execution results. Phone launch, connection, authentication, background behavior and lifecycle checks remain **Not run** until verified on an Android device. Existing 1.4.0 native provenance and protocol/import limitations remain applicable.

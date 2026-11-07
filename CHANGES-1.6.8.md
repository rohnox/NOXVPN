# Special VPN 1.6.8

- Fix the Quick Settings connection toggle crashing the main process with BadParcelableException during VPN handover or stop.
- Marshal handover callbacks and dispatch receipts as framework ResultReceiver endpoints. Anonymous application callback classes no longer need to be resolved by the receiving Messenger Bundle's framework class loader.
- Keep native teardown acknowledgements, dispatch ordering, package/signing identity, profiles and navigation unchanged.

The regression test serializes and reads callback Bundles using the framework class loader on API 24, 31 and 35, and verifies the callback is delivered. Physical-device results and remaining checks are recorded in VALIDATION-1.6.8.md when performed.

# Special VPN 1.6.4

- Repository renamed to sabialballa/SpecialVPN. About/source/privacy and update API/download validation now use the new repository path. No token is embedded. Earlier APKs require one direct installation to migrate their hardcoded download-path validation.
- A successful manual subscription update schedules the existing real-service delay test after group reload. This uses onlyTcp=false, respects active tests/auto-connect, skips empty server lists, and shares the lifecycle-owned entry test job. Failed/skipped/missing updates do not schedule a test. The quiet entry update keeps its existing single test.
- Package, signing key, protocols, native libraries and provider UTC history behavior remain unchanged.

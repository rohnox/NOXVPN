# F-Droid Packaging Notes

This document tracks what is needed to submit OConnect to the official
F-Droid repository.

## Current Status

- Not submitted to F-Droid yet.
- Upstream metadata is being added under `fastlane/metadata/android/en-US`.
- GitHub releases and Android `versionName` are aligned.
- The Android application ID is `io.pengyue.oconnect`.

## Important Finding

F-Droid already has an active OpenConnect package at
`net.openconnect_vpn.android`, and older F-Droid metadata/history exists for
`app.openconnect`.

Because this project is a fork, the submission path is:

1. Keep the user-facing name distinct: **OConnect**.
2. Keep the Android application ID distinct from existing OpenConnect packages.
3. Keep affected authorities, permissions, remote API strings, and metadata
   aligned with that ID.

Application ID:

```text
io.pengyue.oconnect
```

Changing the application ID means this installs as a new app and does not
upgrade any previously installed `app.openconnect` or
`io.pengyue.openconnectnext` build. Before F-Droid submission, test the VPN
service, file provider authority, remote API permission, and Android
backup/migration behavior.

## Official Submission Path

1. Confirm the repo is public and contains the full buildable source.
2. Confirm all dependencies and build tools are FOSS-compatible.
3. Keep license files in the root repo.
4. Add upstream metadata:

   ```text
   fastlane/metadata/android/en-US/title.txt
   fastlane/metadata/android/en-US/short_description.txt
   fastlane/metadata/android/en-US/full_description.txt
   fastlane/metadata/android/en-US/changelogs/<versionCode>.txt
   fastlane/metadata/android/en-US/images/icon.png
   fastlane/metadata/android/en-US/images/phoneScreenshots/1.png
   ```

5. Create a release tag that matches the Android `versionName`.
6. Fork `fdroiddata`.
7. Add a metadata file named after the final application ID.
8. Open a merge request to `fdroiddata`.
9. Respond to maintainer review and build failures.

## Metadata Draft

This is a starting point for the eventual `fdroiddata` metadata file.

```yaml
Categories:
  - Connectivity
License: GPL-2.0-or-later
AuthorName: pengyue-polaron
SourceCode: https://github.com/pengyue-polaron/openconnect-next-android
IssueTracker: https://github.com/pengyue-polaron/openconnect-next-android/issues
Changelog: https://github.com/pengyue-polaron/openconnect-next-android/releases

RepoType: git
Repo: https://github.com/pengyue-polaron/openconnect-next-android.git

Builds:
  - versionName: 1.12.0
    versionCode: 1123
    commit: v1.12.0
    submodules: true
    gradle:
      - yes

AutoUpdateMode: Version
UpdateCheckMode: Tags
CurrentVersion: 1.12.0
CurrentVersionCode: 1123
```

Before submitting this metadata, update `versionName`, `versionCode`, `commit`,
`CurrentVersion`, and `CurrentVersionCode` to the final release that contains
the `io.pengyue.oconnect` application ID.

## Pre-Submission Checklist

- [x] Change application ID from `app.openconnect`.
- [ ] Update app label, About page, README, and fastlane title consistently.
- [ ] Decide whether GitHub releases should remain debug-signed or switch to
      a reproducible release signing process.
- [x] Align Gradle `versionName` with the public release tag.
- [x] Increment `versionCode`.
- [ ] Build from a clean clone with submodules.
- [ ] Run `./gradlew assembleDebug testDebugUnitTest`.
- [ ] Test profile creation, connection prompt, password visibility, log tab,
      and dark mode.
- [ ] Prepare a fresh release tag for F-Droid.

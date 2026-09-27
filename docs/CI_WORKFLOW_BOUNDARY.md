# Fork CI failure correction

The two latest failures reported by the user were Image Minimizer runs 36292508835 and 36292640273. The job reached actions/create-github-app-token and failed because client-id/app-id was empty. This is inherited Fossify repository-maintenance automation, not an Android compile or device-test failure. The customized APK run 36292143974 passed independently.

Removing inherited automation only on security/offline-shifts-v1 was insufficient: issue_comment and pull_request_target read workflow definitions from the default branch. This maintenance change removes the upstream-only workflows from this fork's main branch as well, without merging or changing the preview application's source. The original upstream repository is unchanged. No upstream credentials need to be requested or copied.

A fork-local read-only policy check prevents accidental restoration of those workflows, inherited secrets, or pull_request_target automation. The actual APK/test workflow remains on security/offline-shifts-v1 and is not disabled. Historical failed runs are preserved as evidence; they are not rewritten as successful.

Reference: https://docs.github.com/en/actions/reference/workflows-and-actions/events-that-trigger-workflows

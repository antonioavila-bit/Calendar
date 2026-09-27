"""Fail if upstream-only privileged automation is accidentally restored in this fork."""
from pathlib import Path

root = Path(__file__).resolve().parents[1]
blocked_names = {
    'holiday-generator.yml', 'image-minimizer.yml', 'no-response.yml',
    'pr-labeler.yml', 'pr.yml', 'prepare-release-pr.yml', 'release.yml',
    'testing-build.yml', 'update-commons.yml', 'update-lint-baselines.yml',
    'validate-fastlane-metadata.yml',
}
errors = []
for path in sorted((root / '.github/workflows').glob('*.y*ml')):
    text = path.read_text(encoding='utf-8')
    if path.name in blocked_names:
        errors.append(f'{path.name}: upstream-only workflow must remain removed')
    for token in ('FossifyOrg/.github/.github/workflows/', 'secrets: inherit', 'pull_request_target:'):
        if token in text:
            errors.append(f'{path.name}: forbidden upstream/privileged workflow configuration: {token}')
if errors:
    raise SystemExit('\n'.join(errors))
print('PASS: fork workflows do not call upstream privileged automation or inherit upstream secrets.')

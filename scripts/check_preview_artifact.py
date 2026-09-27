#!/usr/bin/env python3
"""Verify delivered assets and launch/backup flags from the built APK, not just source."""
import hashlib
import pathlib
import re
import sys
import zipfile
apk = pathlib.Path(sys.argv[1])
with zipfile.ZipFile(apk) as archive:
    icons = [n for n in archive.namelist() if n.endswith('/shift_brand.webp')]
    assert len(icons) == 1, f'Approved icon missing/ambiguous: {icons}'
    actual = hashlib.sha256(archive.read(icons[0])).hexdigest()
    assert actual == 'c62ad97916c00194c5f1c82d313e9884b5f5c5454fcf85924084301b01550cc7', actual
manifest = pathlib.Path('deliverables/apk-manifest.txt').read_text()
assert 'android:allowBackup' in manifest and re.search(r'android:allowBackup[^\n]*=(?:false|0x0|\(type 0x12\)0x0)', manifest), 'Cloud backup not disabled in actual APK'
assert 'org.fossify.calendar.security.ShiftActivity' in manifest
assert 'CalDAV' not in manifest and 'CalendarPickerActivity' not in manifest
print('PASS: approved launcher bitmap, offline launcher, and disabled automatic backup')

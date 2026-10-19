#!/usr/bin/env python3
# SPDX-License-Identifier: GPL-3.0-only
"""Check install-name isolation and preservation on actual Facebook/Messenger APKs."""
import re
import subprocess
import sys
import zipfile

AAPT, original_fb, original_ms, patched_fb, patched_ms = sys.argv[1:]

def manifest(apk):
    dump = subprocess.check_output([AAPT, 'dump', 'xmltree', apk, 'AndroidManifest.xml'], text=True)
    nodes, stack = [], []
    for line in dump.splitlines():
        match = re.match(r'(\s*)E: (\S+)', line)
        if match:
            indent = len(match[1])
            while stack and stack[-1][0] >= indent:
                stack.pop()
            node = {'tag': match[2], 'attrs': {}}
            nodes.append(node)
            stack.append((indent, node))
            continue
        match = re.match(r'\s*A: ([^=(]+)(?:\([^)]*\))?=(.*)', line)
        if match and stack:
            raw = match[2]
            quoted = re.match(r'"(.*?)"', raw)
            stack[-1][1]['attrs'][match[1]] = quoted[1] if quoted else raw
    return nodes

def declarations(nodes):
    return {n['attrs']['android:name']: n['attrs']['android:protectionLevel']
            for n in nodes if n['tag'] == 'permission'}

def authorities(nodes):
    return {s for n in nodes if n['tag'] == 'provider'
            for s in n['attrs'].get('android:authorities', '').split(';') if s}

def redirect(pkg, name):
    return {'com.facebook.permission.prod.FB_APP_COMMUNICATION': pkg + '.dtinh.permission.prod.FB_APP_COMMUNICATION',
            'com.facebook.receiver.permission.ACCESS': pkg + '.dtinh.receiver.permission.ACCESS'}.get(name, name)

originals = [manifest(original_fb), manifest(original_ms)]
patched = [manifest(patched_fb), manifest(patched_ms)]
for before, after, src, dest in zip(originals, patched, [original_fb, original_ms], [patched_fb, patched_ms]):
    pkg = next(n['attrs']['package'] for n in before if n['tag'] == 'manifest')
    assert next(n['attrs']['package'] for n in after if n['tag'] == 'manifest') == pkg
    assert declarations(after) == {redirect(pkg, key): value for key, value in declarations(before).items()}
    assert authorities(before) == authorities(after), 'Changed provider authorities'
    for tag in ('activity', 'activity-alias', 'service', 'receiver', 'provider'):
        assert [n['attrs']['android:name'] for n in before if n['tag'] == tag] == [n['attrs']['android:name'] for n in after if n['tag'] == tag], 'Added/renamed component'
    assert not any(v in ('com.facebook.permission.prod.FB_APP_COMMUNICATION', 'com.facebook.receiver.permission.ACCESS')
                   for n in after for v in n['attrs'].values())
    receiver = [n for n in after if n['tag'] == 'receiver' and n['attrs']['android:name'] == 'com.google.firebase.iid.FirebaseInstanceIdReceiver']
    assert len(receiver) == 1
    assert receiver[0]['attrs']['android:enabled'] == '(type 0x12)0xffffffff'
    assert receiver[0]['attrs']['android:permission'] == 'app.revanced.android.c2dm.permission.SEND'
    metadata = {n['attrs']['android:name']: n['attrs'].get('android:value') for n in after if n['tag'] == 'meta-data'}
    assert metadata['app.revanced.MICROG_PACKAGE_NAME'] == 'app.revanced.android.gms'
    assert metadata['app.revanced.android.gms.SPOOFED_PACKAGE_NAME'] == pkg
    assert re.fullmatch('[0-9a-fA-F]{40}', metadata['app.revanced.android.gms.SPOOFED_PACKAGE_SIGNATURE'])
    with zipfile.ZipFile(src) as z1, zipfile.ZipFile(dest) as z2:
        native = [n for n in z1.namelist() if n.startswith('lib/') and n.endswith('.so')]
        assert native == [n for n in z2.namelist() if n.startswith('lib/') and n.endswith('.so')]
        for name in native:
            assert z1.read(name) == z2.read(name), 'Native library changed: ' + name
    print('PASS:', pkg, 'identity/components/authorities/native preserved; signature permissions isolated; microG receiver enabled')

for left, right in ((patched[0], originals[1]), (originals[0], patched[1]), (patched[0], patched[1])):
    assert not declarations(left).keys() & declarations(right).keys(), 'Duplicate permission ownership'
    assert not authorities(left) & authorities(right), 'Duplicate provider ownership'
print('PASS: no duplicate permission declarations or provider authorities for either original/patched order or both patched')

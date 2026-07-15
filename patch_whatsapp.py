#!/usr/bin/env python3
"""
patch_whatsapp.py

Script Python untuk mem-patch WhatsApp APK menggunakan Stitch library.
Meng-inject modul smali_generator (code Java hooks) ke dalam APK target.

Usage:
    python patch_whatsapp.py -p ./WhatsApp.apk -o ./PatchedWhatsApp.apk

Requirement:
    pip install stitch cryptography
    smali_generator/smali_generator.apk harus tersedia (build via GitHub Actions)
"""

import argparse
import os
import shutil
import sys
import tempfile
from pathlib import Path

from stitch import Stitch
from stitch.common import ExternalModule


def get_args():
    parser = argparse.ArgumentParser(
        description="Patch WhatsApp APK dengan fitur mod"
    )
    parser.add_argument(
        "-p", "--apk-path",
        dest="apk_path",
        help="Path ke APK WhatsApp asli",
        required=True
    )
    parser.add_argument(
        "-o", "--output",
        dest="output",
        help="Path output APK hasil patch",
        required=False,
        default="PatchedWhatsApp.apk"
    )
    parser.add_argument(
        "-t", "--temp",
        dest="temp_path",
        help="Folder temporary",
        required=False,
        default=None
    )
    parser.add_argument(
        "--no-sign",
        dest="no_sign",
        action="store_true",
        help="Jangan sign APK output"
    )
    return parser.parse_args()


def main():
    args = get_args()

    base_dir = Path(__file__).parent.resolve()
    smali_generator_dir = base_dir / "smali_generator"
    apk_path = Path(args.apk_path).resolve()
    output_path = Path(args.output).resolve()

    if not apk_path.exists():
        print(f"[ERROR] APK tidak ditemukan: {apk_path}")
        sys.exit(1)

    # Pastikan smali_generator.apk sudah ada (hasil build GitHub Actions)
    smali_apk = smali_generator_dir / "smali_generator.apk"
    if not smali_apk.exists():
        print(f"[WARNING] smali_generator.apk tidak ditemukan di {smali_apk}")
        print("[INFO] Pastikan sudah build atau download dari GitHub Actions artifact")

    # Gunakan temp folder absolut agar path relatif tidak bermasalah
    if args.temp_path:
        temp_dir = Path(args.temp_path)
    else:
        temp_dir = Path(tempfile.gettempdir()) / "wa_patch_temp"
    if temp_dir.exists():
        shutil.rmtree(temp_dir)

    external_modules = [ExternalModule(
        smali_generator_dir,
        'invoke-static {}, Lcom/smali_generator/TheAmazingPatch;->on_load()V'
    )]

    print(f"[+] Starting patch process...")
    print(f"    APK : {apk_path}")
    print(f"    Out : {output_path}")
    print(f"    Temp: {temp_dir}")

    with Stitch(
        apk_path=str(apk_path),
        output_apk=str(output_path),
        temp_path=str(temp_dir),
        external_modules=external_modules,
        should_sign=not args.no_sign,
        extra_artifacts={}
    ) as stitch:
        stitch.patch()

    print(f"[+] Patch selesai: {output_path}")


if __name__ == "__main__":
    main()

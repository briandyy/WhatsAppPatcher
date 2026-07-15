#!/usr/bin/env python3
"""
patch_whatsapp.py

Script Python untuk mem-patch WhatsApp APK menggunakan Stitch library.
Meng-inject modul smali_generator (code Java hooks) ke dalam APK target.

Usage:
    python patch_whatsapp.py -p ./WhatsApp.apk -o ./PatchedWhatsApp.apk

Requirements:
    - Python 3.11+
    - Java JDK 17+ (untuk gradle build smali_generator)
    - stitch>=1.0.1 (pip install stitch)
"""

import argparse
import subprocess
import sys
from pathlib import Path

from stitch import Stitch
from stitch.common import ExternalModule

# Import artifactory generators baru (artinya: class finder untuk resolve placeholder)
from artifactory_generator.view_once_stealth import ViewOnceStealthFinder
from artifactory_generator.revoke_msg import RevokeMessageFinder
from artifactory_generator.group_status import GroupStatusFinder
from artifactory_generator.send_read_receipt import SendReadReceiptFinder


def build_smali_generator() -> None:
    """
    Build Android Gradle project smali_generator untuk menghasilkan APK modul.
    APK output akan bernama smali_generator.apk di root folder smali_generator.
    """
    smali_dir = Path(__file__).parent / "smali_generator"
    if not smali_dir.exists():
        print("[ERROR] smali_generator directory not found!")
        sys.exit(1)

    print("[+] Building smali_generator with Gradle...")
    try:
        # Pakai ./gradlew assembleRelease
        result = subprocess.run(
            [str(smali_dir / "gradlew"), "-p", str(smali_dir), "assembleRelease"],
            capture_output=True,
            text=True,
            check=True
        )
        print(result.stdout)
    except subprocess.CalledProcessError as e:
        print("[ERROR] Gradle build failed:")
        print(e.stdout)
        print(e.stderr)
        sys.exit(1)

    apk = smali_dir / "smali_generator.apk"
    if not apk.exists():
        print("[ERROR] smali_generator.apk not found after build!")
        sys.exit(1)

    print(f"[+] smali_generator.apk built: {apk.resolve()}")


def get_args():
    parser = argparse.ArgumentParser(
        description="Patch WhatsApp APK dengan fitur mod (anti-delete, view-once stealth, dll)"
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
        help="Folder temporary untuk ekstrak konten APK",
        required=False,
        default="./temp"
    )
    parser.add_argument(
        "--no-build",
        dest="should_build",
        help="Skip gradle build (asumsikan smali_generator.apk sudah ada)",
        action="store_false",
        required=False,
        default=True
    )
    parser.add_argument(
        "--no-sign",
        dest="should_sign",
        help="Jangan sign APK output",
        action="store_false",
        required=False,
        default=True
    )
    parser.add_argument(
        "--artifactory",
        dest="artifactory",
        help="Json file artifactory hasil generate",
        required=False,
        default="./artifactory.json"
    )
    return parser.parse_args()


def main():
    args = get_args()

    if args.should_build:
        build_smali_generator()

    # Definisikan artifactory list — setiap finder otomatis scan smali WhatsApp
    # untuk menentukan nama class/method obfuscated yang benar.
    artifactory_list = [
        ViewOnceStealthFinder(args),
        RevokeMessageFinder(args),
        GroupStatusFinder(args),
        SendReadReceiptFinder(args),
        # Tambahkan finder lain jika diperlukan
    ]

    # ExternalModule: inject modul kita ke APK WhatsApp.
    # Stitch akan menambahkan baris invoke-static ke Application.onCreate()
    # yang memanggil TheAmazingPatch.on_load().
    external_modules = [
        ExternalModule(
            Path(__file__).parent / "./smali_generator",
            "invoke-static {}, Lcom/smali_generator/TheAmazingPatch;->on_load()V"
        )
    ]

    print("[+] Starting Stitch patch process...")
    with Stitch(
        apk_path=args.apk_path,
        output_apk=args.output,
        temp_path=args.temp_path,
        artifactory_list=artifactory_list,
        external_modules=external_modules,
        should_sign=args.should_sign,
    ) as stitch:
        stitch.patch()

    print(f"[+] SUCCESS! Patched APK saved to: {Path(args.output).resolve()}")
    print("[WARNING] Patched APK must be signed with same key if updating existing install.")


if __name__ == "__main__":
    main()

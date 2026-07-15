import re
from stitch.artifactory_generator.SimpleArtifactoryFinder import SimpleArtifactoryFinder, CLASS_NAME_RE


class ViewOnceStealthFinder(SimpleArtifactoryFinder):
    """
    Mencari method kirim receipt "viewed" untuk view-once media.
    Pattern smali: method yang referenced oleh string "viewed" atau "receipt"
    dalam class yang handle message status.
    """

    SEND_RECEIPT_RE = re.compile(
        r'\.method public (?:static )?(?P<method_name>\w+)(?P<sig>\(.*\).*)'
    )

    def __init__(self, args):
        super().__init__(args)
        self.is_once = True
        self.is_found = False

    def class_filter(self, class_data: str) -> bool:
        # Cari class yang ada field/message-related status
        return '"viewOnce_"' in class_data or '"receipt"' in class_data or '"viewed"' in class_data

    def extract_artifacts(self, artifacts: dict, class_data: str) -> None:
        # Sederhana: cari method yang ambil dua parameter object (messageInfo, userJid)
        # dan return void — biasanya send receipt
        matches = list(self.SEND_RECEIPT_RE.finditer(class_data))
        if len(matches) < 1:
            return
        # Pilih method pertama yang cocok
        artifacts['VIEW_ONCE_SEND_RECEIPT_CLASS'] = (
            CLASS_NAME_RE.match(class_data).groupdict().get('name').replace('/', '.')
        )
        artifacts['VIEW_ONCE_SEND_RECEIPT_METHOD'] = matches[0].groupdict().get('method_name')
        artifacts['VIEW_ONCE_SEND_RECEIPT_SIG'] = matches[0].groupdict().get('sig')
        self.is_found = True

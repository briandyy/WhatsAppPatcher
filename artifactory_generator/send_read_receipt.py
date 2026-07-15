import re
from stitch.artifactory_generator.SimpleArtifactoryFinder import SimpleArtifactoryFinder, CLASS_NAME_RE


class SendReadReceiptFinder(SimpleArtifactoryFinder):
    """
    Mencari method send read receipt (blue tick).
    Pattern: class yang punya method dengan parameter messageId atau Jid,
    biasanya ada string "read" atau "receipt".
    """

    RECEIPT_RE = re.compile(
        r'\.method public (?:static )?(?P<method_name>\w+)(?P<sig>\(.*\).*)'
    )

    def __init__(self, args):
        super().__init__(args)
        self.is_once = True
        self.is_found = False

    def class_filter(self, class_data: str) -> bool:
        return '"read"' in class_data and '"receipt"' in class_data

    def extract_artifacts(self, artifacts: dict, class_data: str) -> None:
        matches = list(self.RECEIPT_RE.finditer(class_data))
        if len(matches) < 1:
            return
        artifacts['SEND_READ_RECEIPT_CLASS'] = (
            CLASS_NAME_RE.match(class_data).groupdict().get('name').replace('/', '.')
        )
        artifacts['SEND_READ_RECEIPT_METHOD'] = matches[0].groupdict().get('method_name')
        artifacts['SEND_READ_RECEIPT_SIG'] = matches[0].groupdict().get('sig')
        self.is_found = True

import re
from stitch.artifactory_generator.SimpleArtifactoryFinder import SimpleArtifactoryFinder, CLASS_NAME_RE


class RevokeMessageFinder(SimpleArtifactoryFinder):
    """
    Mencari method revoke/delete message untuk anti-delete.
    Pattern: class yang handle protocol message type REVOKE (type 0 biasanya).
    """

    REVOKE_RE = re.compile(
        r'\.method public (?:static )?(?P<method_name>\w+)(?P<sig>\(.*\).*)'
    )

    def __init__(self, args):
        super().__init__(args)
        self.is_once = True
        self.is_found = False

    def class_filter(self, class_data: str) -> bool:
        return '"protocolMessage_"' in class_data or '"REVOKE"' in class_data

    def extract_artifacts(self, artifacts: dict, class_data: str) -> None:
        matches = list(self.REVOKE_RE.finditer(class_data))
        if len(matches) < 1:
            return
        artifacts['REVOKE_MSG_CLASS'] = (
            CLASS_NAME_RE.match(class_data).groupdict().get('name').replace('/', '.')
        )
        artifacts['REVOKE_MSG_METHOD'] = matches[0].groupdict().get('method_name')
        artifacts['REVOKE_MSG_SIG'] = matches[0].groupdict().get('sig')
        self.is_found = True

import re
from stitch.artifactory_generator.SimpleArtifactoryFinder import SimpleArtifactoryFinder, CLASS_NAME_RE


class GroupStatusFinder(SimpleArtifactoryFinder):
    """
    Mencari method AB test dan permission check untuk Group Status.
    Pattern: class yang ada string "group_status", "groupStatus", atau "community_status".
    """

    FLAG_RE = re.compile(
        r'\.method public (?:static )?(?P<method_name>\w+)(?P<sig>\(.*\).*)'
    )

    def __init__(self, args):
        super().__init__(args)
        self.is_once = True
        self.is_found = False

    def class_filter(self, class_data: str) -> bool:
        return 'group_status' in class_data.lower() or 'groupstatus' in class_data.lower()

    def extract_artifacts(self, artifacts: dict, class_data: str) -> None:
        matches = list(self.FLAG_RE.finditer(class_data))
        if len(matches) < 1:
            return
        class_name = CLASS_NAME_RE.match(class_data).groupdict().get('name').replace('/', '.')
        method_name = matches[0].groupdict().get('method_name')
        method_sig = matches[0].groupdict().get('sig')

        artifacts['GROUP_STATUS_AB_CLASS'] = class_name
        artifacts['GROUP_STATUS_AB_METHOD'] = method_name
        artifacts['GROUP_STATUS_AB_SIG'] = method_sig

        artifacts['GROUP_STATUS_PERM_CLASS'] = class_name
        artifacts['GROUP_STATUS_PERM_METHOD'] = method_name
        artifacts['GROUP_STATUS_PERM_SIG'] = method_sig

        artifacts['GROUP_STATUS_UI_CLASS'] = class_name
        artifacts['GROUP_STATUS_UI_METHOD'] = method_name
        artifacts['GROUP_STATUS_UI_SIG'] = method_sig

        self.is_found = True

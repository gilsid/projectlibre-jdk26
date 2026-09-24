#!/usr/bin/env bash
set -euo pipefail

APP_VERSION="@version@"
JPACKAGE="${JPACKAGE:-jpackage}"
if [[ -n "${JAVA_HOME:-}" && -x "${JAVA_HOME}/bin/jpackage" ]]; then
    JPACKAGE="${JAVA_HOME}/bin/jpackage"
fi

rm -rf app
mkdir -p app
rm -f "ProjectLibre-${APP_VERSION}.deb"
"${JPACKAGE}" --type deb --input source --dest app --name ProjectLibre \
    --main-jar "projectlibre-${APP_VERSION}.jar" \
    --icon source/projectlibre.png \
    --app-version "${APP_VERSION}" \
    --license-file source/license/license.txt \
    --vendor "ProjectLibre" \
    --linux-package-name projectlibre \
    --linux-shortcut \
    --linux-menu-group "Utility"


ENABLED=${ENABLED:-true}

echo "import DownloadFile from '../download-ifc';"
echo "export let download : DownloadFile = {"
echo "    title: \"$TITLE\","
echo "    description: \"$DESCRIPTION\","
echo "    href: \"$HREF\","
echo "    help: \"$HREF_HELP\","
echo "    size: \"$SIZE\","
echo "    enabled: $ENABLED,"
echo "    created: \"$CREATED\""
echo "}"

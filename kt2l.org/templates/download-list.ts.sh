
idx=0
for e in $(echo $ENTRIES); do
    f="${e%.*}"
    echo "import {download as d$idx} from './$f'";
    let idx=$idx+1
done
echo ""
echo "export const downloadList = ["
idx=0
for e in $(echo $ENTRIES); do
    echo "d$idx,";
    let idx=$idx+1
done
echo "];"
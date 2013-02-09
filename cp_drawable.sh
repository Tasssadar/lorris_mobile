#!/bin/bash
FOLDER_LIGHT="/home/tassadar/Android_Design_Icons_20120229/All_Icons/holo_light/"
FOLDER_DARK="/home/tassadar/Android_Design_Icons_20120229/All_Icons/holo_dark/"
LORRIS_FOLDER="/home/tassadar/lorris_mobile"
DARK=1
FOLDER=""
DPI_LIST=( mdpi hdpi xhdpi )


for arg in "$@"
do
    if [ $arg == "-l" ] ; then
        DARK=0
    elif [ $arg == "-d" ] ; then
        DARK=1
    fi
done

if [ $DARK -eq 1 ] ; then
    echo "Using dark holo"
    FOLDER=$FOLDER_DARK
else
    echo "Using light holo"
    FOLDER=$FOLDER_LIGHT
fi

echo ""

for arg in "$@"
do
    if [ $arg == "-l" ] || [ $arg == "-d" ] ; then
        continue
    fi

    new_name="$(echo $arg | cut -d'-' -f2-999 | tr '-' '_')"
    

    echo "$arg to $new_name..."

    for dpi in ${DPI_LIST[@]}
    do
        name=$new_name
        if [ -e $LORRIS_FOLDER/res/drawable-$dpi/$name ]; then
            if [ $DARK -eq 1 ]; then
                name="${name%.png}_dark.png"
            else
                name="${name%.png}_light.png"
            fi
        fi
        cmd="cp $FOLDER$dpi/$arg $LORRIS_FOLDER/res/drawable-$dpi/$name"
        echo "$cmd"
        $($cmd)
    done
done

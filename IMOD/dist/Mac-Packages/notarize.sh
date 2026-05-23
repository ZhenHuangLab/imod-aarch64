#!/bin/bash

# Script to upload a notarization request on the pkg file in build, wait until it
# is approved, and staple the notarization to the package

# Require these variables to be defined
if [ -z "${IMOD_MAC_DEV_INST_ID}" ] ; then exit 0 ; fi

if [ -z "${IMOD_ALTOOL_USERNAME}" ] ; then
    echo "Both IMOD_MAC_DEV_INST_ID and IMOD_ALTOOL_USERNAME need to be defined to use this script"
    exit 0
fi

packagesbuild --identity "${IMOD_MAC_DEV_INST_ID}" Installer.pkgproj
if [ $? != "0" ] ; then
  echo "Building Mac installer failed"
  exit 1
fi

# Request the notarization and check the result
xcrun altool --notarize-app \
  --primary-bundle-id edu.colorado.imod \
  --username $IMOD_ALTOOL_USERNAME \
  --password "@keychain:ALTOOL_PASSWORD" \
  --file build/*.pkg > request.out 2>&1

grep -q "No errors uploading" request.out
if [ $? != "0" ] ; then
    echo "altool had an error uploading the package for notarization"
    exit 1
fi

# Get the UUID from it
UUID=`sed -n '/RequestUUID = /s///p' request.out`
if [ -z $UUID ] ; then
    echo "Could not get UUID from package notarization request"
    exit 1
fi

# Get the info and wait until status is success; other than in progress is failure
# This is 30 minutes, it took < 3 minutes for package with no internal signing
counter=0
notFound=0
while [  $counter -lt 360 ]; do
    let counter=counter+1 
    xcrun altool --notarization-info $UUID --username "$IMOD_ALTOOL_USERNAME" --password "@keychain:ALTOOL_PASSWORD" > info.out 2>&1
    grep -q "Status: success" info.out
    if [ $? == "0" ] ; then

        # Staple when it succeeds, do not count it as failure if this fails
        xcrun stapler staple build/*.pkg > staple.out 2>&1
        grep -q "action worked" staple.out
        if [ $? == "0" ] ; then
            echo "Stapling of package notarization succeeded"
        else
            echo "Package notarization succeeded but stapling failed"
        fi
        if [ -n "$1" ] ; then
            scp build/*.pkg $1
        fi
        exit 0
    fi

    grep -q "Status:.*progress" info.out
    if [ $? != "0" ] ; then

        # This has been seen to occur on the first xcrun, but check up to 10 times
        # before failing.  Anything else would be a failure
        grep -qi "could not find the request" info.out
        if [[ $? == "0" && $notFound < 10 ]] ; then
            ((notFound=notFound+1))
        else
            echo "Package notarization request failed"
            exit 1
        fi
    fi
    sleep 5
done
echo "Time out waiting for package notarization request to finish"
exit 1


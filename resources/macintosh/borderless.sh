#!/bin/bash
# Run Borderless

# TODO: I think that the /Library/LaunchAp-- .plist file is failing to call the
# `lein run` command. So I need to run it with a direct path. It's currently
# not working as implemented.

set -ex
echo "Setting Up Borderless"
# export PATH=$PATH:/usr/local/bin/lein
cd /Users/schmudde/Work/borderless/borderless/ && /usr/local/bin/lein run > borderless-log.txt 2>&1

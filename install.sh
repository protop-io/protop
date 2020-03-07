#!/bin/bash

echo ""
echo "--------------------"
echo "protop dev installer"
echo "--------------------
"

protop_say()
{
    echo "[protop]  $1"
}

#build a clean distribution
gradle=./gradlew
protop_say "\`gradle clean\`"
$gradle clean -q

protop_say "\`gradle build\`"
$gradle build -q

# move distribution
dir=~/.protop
tmp=$dir/tmp

protop_say "Moving build artifacts to temporary directory"

mkdir -p $tmp
rm -rf $tmp/*
dist=$tmp/dist.zip
cp ./protop-cli/build/distributions/*.zip $dist
cd $tmp

# unpack
unzip -qq $dist
protop_say "Deleting previous dev installation"

rm -rf $dir/dev
mkdir $dir/dev

mv $tmp/*/* $dir/dev/
mv $dir/dev/bin/protop-cli $dir/dev/bin/protop-dev
mv $dir/dev/bin/protop-cli.bat $dir/dev/bin/protop-dev.bat
cd $dir && rm -rf $tmp

if [[ ":$PATH:" == *":$HOME/.protop/dev/bin:"* ]]; then
    protop_say "\`~/.protop/dev/bin\` already in PATH"
    protop_say "Finished! Try \`protop-dev\` or \`protop-dev help\` to get started!"
else
    protop_say "Your path is missing \`~/.protop/dev/bin\`; you will need to add it:"
    protop_say "  - Add \`export PATH=\"\$PATH:\$HOME/.protop/dev/bin\"\` to your \`~/.bashrc\` or \`~/.zshrc\`."
    protop_say "  - Then try \`protop-dev\` or \`protop-dev help\` to get started!"
fi

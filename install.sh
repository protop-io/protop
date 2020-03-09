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
protop_say "Deleting previous installation"

rm -rf $dir/bin
mkdir $dir/bin

mv $tmp/*/* $dir/
mv $dir/bin/protop-cli $dir/bin/protop
mv $dir/bin/protop-cli.bat $dir/bin/protop.bat
cd $dir && rm -rf $tmp

if [[ ":$PATH:" == *":$HOME/.protop/bin:"* ]]; then
    protop_say "\`~/.protop/bin\` already in PATH"
    protop_say "Finished! Try \`protop\` or \`protop help\` to get started!"
else
    protop_say "Your path is missing \`~/.protop/bin\`; you will need to add it:"
    protop_say "  - Add \`export PATH=\"\$PATH:\$HOME/.protop/bin\"\` to your \`~/.bashrc\` or \`~/.zshrc\`."
    protop_say "  - Then try \`protop\` or \`protop help\` to get started!"
fi

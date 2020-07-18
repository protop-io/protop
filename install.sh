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
$gradle build -Pdev -q

# move distribution
dir=~/.protop
tmp=$dir/tmp

protop_say "Moving build artifacts to temporary directory"
mkdir -p $tmp
rm -rf $tmp/*
dist=$tmp/dist.tgz
cp ./build/distributions/*.tgz $dist
cd $tmp

# unpack
protop_say "Unpacking artifacts"
tar -C . -zxf $dist

protop_say "Deleting previous installation"
rm -rf $dir/dev
mkdir $dir/dev
mkdir $dir/dev/bin
mkdir $dir/dev/lib

protop_say "Moving binaries to their new home"
mv $tmp/*/bin/* $dir/dev/bin/
mv $dir/dev/bin/protop $dir/dev/bin/protop-dev
mv $dir/dev/bin/protop.bat $dir/dev/bin/protop-dev.bat
# and jars
mv $tmp/*/lib/* $dir/dev/lib/

# cleanup
cd $dir && rm -rf $tmp

if [[ ":$PATH:" == *":$HOME/.protop/dev/bin:"* ]]; then
    protop_say "\`~/.protop/dev/bin\` already in PATH"
    protop_say "Succeeded!
"
    protop
else
    protop_say "Your path is missing \`~/.protop/dev/bin\`; you will need to add it:"
    protop_say "  - Add \`export PATH=\"\$PATH:\$HOME/.protop/dev/bin\"\` to your \`~/.bashrc\` or \`~/.zshrc\` etc."
    protop_say "  - Then try \`protop\` or \`protop help\` to get started!"
fi

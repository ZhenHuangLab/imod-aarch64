cd /home/sueh/workspace/ImodTests
hg fe
cd scriptTests/comstest
tests=`./testcopy|grep BATCHRUNTOMO|awk '{print $1}'`
for i in $tests; do ./testcopy $i; done;

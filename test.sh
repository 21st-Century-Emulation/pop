docker build -q -t pop .
docker run --rm --name pop -d -p 8080:8080 -e READ_MEMORY_API=http://localhost:8080/api/v1/debug/readMemory pop

sleep 50 # Just scala things

RESULT=`curl -s --header "Content-Type: application/json" \
  --request POST \
  --data '{"id":"abcd", "opcode":193,"state":{"a":181,"b":10,"c":10,"d":0,"e":0,"h":25,"l":10,"flags":{"sign":false,"zero":false,"auxCarry":false,"parity":false,"carry":false},"programCounter":0,"stackPointer":255,"cycles":0,"interruptsEnabled":true}}' \
  http://localhost:8080/api/v1/execute`
EXPECTED='{"id":"abcd", "opcode":193,"state":{"a":181,"b":255,"c":0,"d":0,"e":0,"h":25,"l":10,"flags":{"sign":false,"zero":false,"auxCarry":false,"parity":false,"carry":false},"programCounter":0,"stackPointer":257,"cycles":10,"interruptsEnabled":true}}'

docker kill pop

DIFF=`diff <(jq -S . <<< "$RESULT") <(jq -S . <<< "$EXPECTED")`

if [ $? -eq 0 ]; then
    echo -e "\e[32mPOP Test Pass \e[0m"
    exit 0
else
    echo -e "\e[31mPOP Test Fail  \e[0m"
    echo "$RESULT"
    echo "$DIFF"
    exit -1
fi
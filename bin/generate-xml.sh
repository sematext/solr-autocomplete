echo "<add>"
while read line; do
  echo "  <doc><field name=\"phrase\"><![CDATA[$line]]></field></doc>"
done;
echo "</add>"

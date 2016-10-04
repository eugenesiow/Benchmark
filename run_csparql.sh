for (( i = 1 ; i <= 12 ; i++ ))  do
	java -cp target/lib/*:target/lib/CQELS-lib/*:target/citybench-0.0.1.jar org.insight_centre.citybench.main.CityBench rate=1.0 frequency=1.0 duration=15m queryDuplicates=1 startDate=2014-08-11T11:00:00 endDate=2014-08-31T11:00:00 engine=csparql query=Q$i.txt
done
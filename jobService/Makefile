check:
	sed -i -e 's#@Ignore#//@Ignore#' src/test/java/de/gunis/roger/jobService/WithPostProcessing.java
	../gradlew test --tests=*withPostprocessing*
	sed -i -e 's#//@Ignore#@Ignore#' src/test/java/de/gunis/roger/jobService/WithPostProcessing.java
	qpdfview /var/tmp/schedule/allEvents.pdf

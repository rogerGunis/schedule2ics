pdf: 
	test -f /var/tmp/schedule/allEvents.pdf && mv /var/tmp/schedule/allEvents.pdf /var/tmp/schedule/allEvents.previous.pdf
	sed -i -e 's#@Ignore#//@Ignore#' jobService/src/test/java/de/gunis/roger/jobService/WithPostProcessing.java
	./gradlew build 
	./gradlew :jobService:test --tests "de.gunis.roger.jobService.WithPostProcessing.withPostprocessing"
	sed -i -e 's#//@Ignore#@Ignore#' jobService/src/test/java/de/gunis/roger/jobService/WithPostProcessing.java
	test -f /var/tmp/schedule/allEvents.pdf && qpdfview /var/tmp/schedule/allEvents.pdf

render:
	google-chrome-stable --headless --disable-gpu --print-to-pdf=/var/tmp/schedule/allEvents.pdf file:///var/tmp/schedule/allEvents.html && qpdfview /var/tmp/schedule/allEvents.pdf

view:
	qpdfview /var/tmp/schedule/allEvents.pdf

clean: 
	./gradlew clean


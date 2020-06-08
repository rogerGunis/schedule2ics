prepare:
	mkdir -p /var/tmp/schedule/

pdf: 
	test -f /var/tmp/schedule/allEvents.pdf && mv /var/tmp/schedule/allEvents.pdf /var/tmp/schedule/allEvents.previous.pdf || true
	sed -i -e 's#^\(\s+\)@Ignore#\1//@Ignore#' jobService/src/test/java/de/gunis/roger/jobService/WithPostProcessing.java
	export GRADLE_HOME=$$HOME/.gradle
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


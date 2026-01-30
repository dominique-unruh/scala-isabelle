view-test-results:
	rm -rf target/tmp
	mkdir target/tmp
	cd target/tmp && gh run download
	xdg-open target/tmp/index.html

publish-snapshot:
	sbt "+ publishSigned"

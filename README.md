# Claim an estate frontend

This service is responsible for navigating an organisation 'individual' user to claim their estate.
The service sends the user to the estates-relationship-establishment-service and allocates their enrolment with EACD using tax-enrolments.

To run locally using the micro-service provided by the service manager:
```
sm2 --start ESTATES_ALL -r
```

If you want to run your local copy, then stop the frontend ran by the service manager and run your local code by using the following (port number is 8830 but is defaulted to that in build.sbt).
```
sbt run
```

### License

This code is open source software licensed under the [Apache 2.0 License]("http://www.apache.org/licenses/LICENSE-2.0.html").

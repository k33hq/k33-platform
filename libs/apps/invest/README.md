# Investment

## User states

| Status         | HTTP status code | Description                                            |
|----------------|------------------|--------------------------------------------------------|
| NOT_REGISTERED | 404 Not found    | For new user (no info found in database)               |
| NOT_AUTHORISED | 403 Forbidden    | For user **NOT** authorised to see fund information    |
| REGISTERED     | 200 OK           | For registered user authorised to see fund information |

## Investor types

* PROFESSIONAL
* ELECTIVE_PROFESSIONAL
* NON_PROFESSIONAL

## Invalid request

```
(Investor type is (PROFESSIONAL or NON_PROFESSIONAL)) and 
  (mandatory field == null or phone number is invalid))
```

### Mandatory fields (for PROFESSIONAL or NON_PROFESSIONAL investor)

* name
* phone number
* country
* fund name

Note: Investor type is mandatory for all cases.

## Business scenarios

| Given state | When event                       | Then Output     | New state      |
|-------------|----------------------------------|-----------------|----------------|
| any         | Invalid request                  | 400 Bad Request | unchanged      |
| any         | Country in denied list           | 403 Forbidden   | NOT_AUTHORISED |
| any         | Incorrect fund name              | 403 Forbidden   | NOT_AUTHORISED |
| any         | NON_PROFESSIONAL                 | 403 Forbidden   | NOT_AUTHORISED |
| any         | PROFESSIONAL or NON_PROFESSIONAL | 200 OK          | REGISTERED     |

## Firestore DB schema

```yaml
# collection
users:
  # document-id: userId is provided by Firebase Auth
  - userId:
    # collection   
    apps:
      # document-id: (appId = Invest) is assigned for Invest app. 
      - invest:
        # collection
        funds:
          # document-id: (fundId = k33-assets-i-fund-limited)
          - k33-assets-i-fund-limited:
            status: NOT_REGISTERED | NOT_AUTHORIZED | REGISTERED
            # collection
            fund-info-requests:
              - <auto-generated-id>:
                investorType: PROFESSIONAL | ELECTIVE_PROFESSIONAL
                name: String?
                company: String?
                phoneNumber:
                  countryCode: String?
                  nationalNumber: String?
                # ISO Alpha-3 3-char code
                countryCode: String?
                fundName: String?
```
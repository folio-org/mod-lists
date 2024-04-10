# 2.0.x

## 2.0.2
- Fix bug where the Missing Items canned list could not be duplicated [MODLISTS-104](https://folio-org.atlassian.net/browse/MODLISTS-104)

## 2.0.1
- Bump folio-s3-client dependency to the latest version ([MODLISTS-96](https://folio-org.atlassian.net/browse/MODLISTS-96))
- Fix bug with multipart uploads of CSVs  ([MODFQMMGR-218](https://folio-org.atlassian.net/browse/MODFQMMGR-218))

## 2.0.0
- Manually flush the entity manager, to address race condition ([MODLISTS-66](https://folio-org.atlassian.net/browse/MODLISTS-66))
- Add list versioning ([MODLISTS-60](https://folio-org.atlassian.net/browse/MODLISTS-60), [MODLISTS-61](https://folio-org.atlassian.net/browse/MODLISTS-61), [MODLISTS-62](https://folio-org.atlassian.net/browse/MODLISTS-62), [MODLISTS-63](https://folio-org.atlassian.net/browse/MODLISTS-63), [MODLISTS-72](https://folio-org.atlassian.net/browse/MODLISTS-72), [MODLISTS-75](https://folio-org.atlassian.net/browse/MODLISTS-75), [MODLISTS-76](https://folio-org.atlassian.net/browse/MODLISTS-76))
- Refactor list deletion to use versioning ([MODLISTS-74](https://folio-org.atlassian.net/browse/MODLISTS-74))
- Update folio-spring-base version ([MODLISTS-71](https://folio-org.atlassian.net/browse/MODLISTS-71))
- Implemented soft deletion of lists ([MODLISTS-71](https://folio-org.atlassian.net/browse/MODLISTS-71) and [MODLISTS-57](https://folio-org.atlassian.net/browse/MODLISTS-57))
- Adjust size constraints on ListEntity fields
- Add empty FQL operator ([MODFQMMGR-119](https://folio-org.atlassian.net/browse/MODFQMMGR-119))
- Fix bug where list fields were getting discarded on update ([MODLISTS-81](https://folio-org.atlassian.net/browse/MODLISTS-81))
- Increase the list refresh query timeout and make it configurable ([MODLISTS-83](https://folio-org.atlassian.net/browse/MODLISTS-83))
- Change record identifier from UUID to list of strings ([MODLISTS-82](https://folio-org.atlassian.net/browse/MODLISTS-82))
- Create user friendly queries for derived columns (#62) ([MODLISTS-85](https://folio-org.atlassian.net/browse/MODLISTS-85))
- Update UserFriendlyQueryService to handle contains_all operators ([MODFQMMGR-131](https://folio-org.atlassian.net/browse/MODFQMMGR-131))
- Use multipart upload for CSV exports ([MODLISTS-87](https://folio-org.atlassian.net/browse/MODLISTS-87))
- Add support for contains_any and not_contains_any operators in UserFriendlyQueryService ([MODFQMMGR-130](https://folio-org.atlassian.net/browse/MODFQMMGR-130))
- Bump the Spring Boot dependency version to 3.2.3 ([MODLISTS-88](https://folio-org.atlassian.net/browse/MODLISTS-88))
- Update name in module descriptor
- Temporarily retrieve contents for all fields in entity type definition ([MODLISTS-90](https://folio-org.atlassian.net/browse/MODLISTS-90))
- Pass fields to export in export request ([MODLISTS-91](https://folio-org.atlassian.net/browse/MODLISTS-91))
- Check existing list of fields on list update ([MODLISTS-94](https://folio-org.atlassian.net/browse/MODLISTS-94))
- Add generic exception handler ([MODLISTS-59](https://folio-org.atlassian.net/browse/MODLISTS-59))

# 1.0.x

## 1.0.4
- Bump the folio-s3-client dependency version to 2.0.5

## 1.0.3
- Handle IllegalArgumentExceptions in requests

## 1.0.2
- Rename canned reports and update their descriptions
- Update the provided `_tenant` interface in the module descriptor to 2.0

## 1.0.1
- Add performance metadata to refreshes
- Update the available fields in the Loan entity type
- Increase column width for FQL queries

## 1.0.0
- Initial release


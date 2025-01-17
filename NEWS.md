# 3.0.x
- Stream export downloads directly without buffering ([MODLISTS-186](https://folio-org.atlassian.net/browse/MODLISTS-186))

## 3.0.7
- Add instance-format permission to mod-lists system user

## 3.0.6
- Use applicable ID columns in user-friendly query generation ([MODLISTS-185])
- Spring Boot 3.3.7, folio-s3-client 2.2.1, aws s3 2.29.47 fixing vulns ([MODLISTS-187])
- Update UserFriendlyQueryService to handle string-based custom fields ([MODFQMMGR-82])

[MODLISTS-185]: https://folio-org.atlassian.net/browse/MODLISTS-185
[MODLISTS-187]: https://folio-org.atlassian.net/browse/MODLISTS-187
[MODFQMMGR-82]: https://folio-org.atlassian.net/browse/MODFQMMGR-82

## 3.0.5
- Check for null description before appending warnings during list migrations ([MODLISTS-180](https://folio-org.atlassian.net/browse/MODLISTS-180))
- Update to lib-fqm-query-processor 3.0.1 to add support for JSONB array fields ([MODFQMMGR-548](https://folio-org.atlassian.net/browse/MODFQMMGR-548))
- Increase description column widths ([MODLISTS-183](https://folio-org.atlassian.net/browse/MODLISTS-183))

## 3.0.4
- Require interface `entity-types v2.0`
- Allow hidden fields in user-friendly query generation ([MODLISTS-175])
- Handle null conditions during user-friendly query generation ([MODLISTS-176])
- Make migration errors non-fatal ([MODLISTS-178])

[MODLISTS-175]: https://folio-org.atlassian.net/browse/MODLISTS-175
[MODLISTS-176]: https://folio-org.atlassian.net/browse/MODLISTS-176
[MODLISTS-178]: https://folio-org.atlassian.net/browse/MODLISTS-178

## 3.0.3
- Retry failed S3 uploads during export ([MODLISTS-158])
- Use custom SystemUserClient only when system user is enabled ([MODLISTS-165])
- Ignore unrecognized fields during export ([MODLISTS-170])

[MODLISTS-158]: https://folio-org.atlassian.net/browse/MODLISTS-158
[MODLISTS-165]: https://folio-org.atlassian.net/browse/MODLISTS-165
[MODLISTS-170]: https://folio-org.atlassian.net/browse/MODLISTS-170

## 3.0.2
- Consider daylight savings when determining dates to show for user-friendly queries ([MODLISTS-161])
- Add a missing interface to the MD ([MODLISTS-160])

[MODLISTS-160]: https://folio-org.atlassian.net/browse/MODLISTS-160
[MODLISTS-161]: https://folio-org.atlassian.net/browse/MODLISTS-161

## 3.0.1
- Use the new /query/contents/privileged API endpoint for exports ([MODFQMMGR-563])
- Retry migrating queries when enabling the module when requests fail due to missing entity type permissions ([MODLISTS-155])
- Synchronize system user permissions in the module descriptor and system-user-permissions.txt ([MODLISTS-157])

[MODFQMMGR-563]: https://folio-org.atlassian.net/browse/MODFQMMGR-563
[MODLISTS-155]: https://folio-org.atlassian.net/browse/MODLISTS-155
[MODLISTS-157]: https://folio-org.atlassian.net/browse/MODLISTS-157

## 3.0.0
- Add allocated resource recommendations to README ([MODLISTS-97](https://folio-org.atlassian.net/browse/MODLISTS-97))
- Always include ID columns in exports
- Restrict list access to lists, based on the user's permissions ([MODLISTS-112](https://folio-org.atlassian.net/browse/MODLISTS-112))
- Handle new entity type respone format from mod-fqm-manager ([MODLISTS-128](https://folio-org.atlassian.net/browse/MODLISTS-128))
- Rename the "lists.item.refresh" permission to follow naming conventions ([MODLISTS-136](https://folio-org.atlassian.net/browse/MODLISTS-136))
- Add support for automatic list query migration ([MODLISTS-126](https://folio-org.atlassian.net/browse/MODLISTS-126))
- Localize dates in user-friendly query ([MODLISTS-145](https://folio-org.atlassian.net/browse/MODLISTS-145))
- Integrate permission name changes from mod-fqm-manager ([MODLISTS-153](https://folio-org.atlassian.net/browse/MODLISTS-153))
- Add retry logic to list query migration during tenant initialization ([MODLISTS-155](https://folio-org.atlassian.net/browse/MODLISTS-155))
- Migrate cross-tenant lists to private ([MODLISTS-152](https://folio-org.atlassian.net/browse/MODLISTS-152))
- Convert dates to local time when exporting lists ([MODLISTS-135](https://folio-org.atlassian.net/browse/MODLISTS-135))

# 2.0.x

## 2.0.6
- Fix artifact version (2.0.5 was accidentally released as a snapshot)

## 2.0.5
- Add system user to handle long-lived exports ([MODLISTS-109](https://folio-org.atlassian.net/browse/MODLISTS-109))

## 2.0.4
- Add a startup check to verify that S3/MinIO are accessible ([MODLISTS-108](https://folio-org.atlassian.net/browse/MODLISTS-108))
- Fix a bug where records deleted after a list refresh caused exports to fail ([MODLISTS-105](https://folio-org.atlassian.net/browse/MODLISTS-105))

## 2.0.3
- Add more detail about S3/MinIO config to the README ([MODLISTS-107](https://folio-org.atlassian.net/browse/MODLISTS-107))
- Only select columns that are visibleByDefault when none are specified in the list ([MODLISTS-98](https://folio-org.atlassian.net/browse/MODLISTS-98))
- Fix bug where data was never retrieved for non-default columns ([MODLISTS-99](https://folio-org.atlassian.net/browse/MODLISTS-99))

## 2.0.2
- Fix bug where the Missing Items canned list could not be duplicated ([MODLISTS-104](https://folio-org.atlassian.net/browse/MODLISTS-104))

## 2.0.1
- Bump folio-s3-client dependency to the latest version ([MODLISTS-96](https://folio-org.atlassian.net/browse/MODLISTS-96))
- Fix bug with multipart uploads of CSVs ([MODFQMMGR-218](https://folio-org.atlassian.net/browse/MODFQMMGR-218))

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


# gcs-uploader

## Introduction

Customers require a user friendly tool that allows non-technical enterprise end-users to upload content to GCP. In media use cases, these uploads can be hundreds of gigabytes in size and are not suitable for upload via browser based interfaces.

The desktop uploader tool is designed for enterprises with non-technical end users who wish to upload large files from their desktops into the cloud on a regular basis. The desktop upload tool has the following features:
* Uploader desktop app
* Uses composable, parallel upload features of GCS
* Uses Google Auth in the cloud to prevent any propagation of sensitive credentials to the desktop app
* UI takes a list of files and folders in any combination and gives the user feedback on the progress of the various files

The system has been designed in a manner such that no sensitive information is shipped with the desktop app. All access to resources and credentials is provided through the Google authentication system. This allows the uploader to be a more secure desktop tool that can only be used by authorized users.

While the initial setup and configuration is a multi-step process for the system administrator, the actual operation of the uploader tool is, by design, exceedingly simple for the end user.

Please read the [User Manual](UserManual.pdf) for detailed instructions on installing and operationalizing the gcs-uploader.

If you have a Google Cloud account team, you can contact them for further help.

### License

gcs-uplaoder is released under the [Apache 2.0 license](LICENSE).

```
Copyright 2008 Google Inc.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
```

### Disclaimer

This is not an officially supported Google product.
DependencyDownloader
====================

DependencyDownloader is a simple and very small Java application for 
downloading project dependencies. Because the very small size it can 
be added to a VCS without problems.

The only requirement to run the downloader is a Java 7+ runtime.

## Features

* Download and extract files or archives
* Check downloaded files against MD5 and SHA1 checksum
* Cleanup/Remove previous downloaded files

## How to Use

Simply create a XML file for example ``depend.xml`` with the following 
content: (you should use a real URL)

```xml
<?xml version="1.0" encoding="utf-8" ?>
<DependencyDownloader xmlns="http://boehmke.net/tools/dependency_downloader/depend">
    <Zip Source="http://example.com/test.zip"
         Destination="tmp/zip_dir/"/>
</DependencyDownloader>
```

After that simply run ``java -jar path/to/DependencyDownloader.jar`` in 
the same directory where the ``depend.xml`` is located. If the XML file 
has an other name you can call 
``java -jar path/to/DependencyDownloader.jar otherName.xml``.

### Supported files & archives

| XML tag | Type                        |
|:--------|:----------------------------|
| File    | Plain file                  |
| GZip    | GZip compressed file        |
| Zip     | ZIP archive                 |
| Tar     | TAR archive (uncompressed)  |
| TarGz   | TAR archive GZip compressed |

### File or Archive attributes

| XML attribute | Description                                                      |
|:--------------|:-----------------------------------------------------------------|
| Source        | Source URL for download                                          |
| Destination   | Destination of downloaded file or extracted archive              |
| Md5           | MD5 checksum of the downloaded file                              |
| Sha1          | SHA1 checksum of the downloaded file                             |
| SourceSubDir  | (Archive only) Sub directory of archive that should be extracted |




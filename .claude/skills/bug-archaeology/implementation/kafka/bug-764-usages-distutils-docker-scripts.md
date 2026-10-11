**Commit**: 675552a724 · **Ticket**: KAFKA-19490
**Subsystem**: docker/common.py
**Category**: Logic error in condition

**Root cause**
The
[distutils](https://docs.python.org/3.13/whatsnew/3.12.html#distutils)
package is removed from Python 3.12. Change `distutils` usage to `shutil`. Reviewers: Mickael Maison <mimaison@apache.org>

---------

Signed-off-by: PoAn Yang <payang@apache.org>

**Before/After**
```java
// Before
from distutils.file_util import copy_file

from distutils.dir_util import copy_tree
    copy_tree(f"{current_dir}/{image_type}", f"{temp_dir_path}/{image_type}")
    copy_tree(f"{current_dir}/resources", f"{temp_dir_path}/{image_type}/resources")
    copy_file(f"{current_dir}/server.properties", f"{temp_dir_path}/{image_type}")
        copy_file(kafka_archive, f"{temp_dir_path}/{image_type}/kafka.tgz")
from distutils.dir_util import copy_tree
        copy_tree(f"{current_dir}/test/fixtures", f"{temp_dir_path}/fixtures")
from distutils.dir_util import copy_tree
    copy_tree(f"{current_dir}/docker_official_images/{kafka_version}/{image_type}",
              f"{temp_dir_path}/{image_type}")

// After
    shutil.copytree(f"{current_dir}/{image_type}", f"{temp_dir_path}/{image_type}", dirs_exist_ok=True)
    shutil.copytree(f"{current_dir}/resources", f"{temp_dir_path}/{image_type}/resources", dirs_exist_ok=True)
    shutil.copy(f"{current_dir}/server.properties", f"{temp_dir_path}/{image_type}")
        shutil.copy(kafka_archive, f"{temp_dir_path}/{image_type}/kafka.tgz")
        shutil.copytree(f"{current_dir}/test/fixtures", f"{temp_dir_path}/fixtures", dirs_exist_ok=True)
    shutil.copytree(f"{current_dir}/docker_official_images/{kafka_version}/{image_type}",
              f"{temp_dir_path}/{image_type}", dirs_exist_ok=True)
    shutil.copytree(f"{current_dir}/docker_official_images/{kafka_version}/jvm/resources",
              f"{temp_dir_path}/{image_type}/resources", dirs_exist_ok=True)
    shutil.copy(f"{current_dir}/server.properties", f"{temp_dir_path}/{image_type}")
    new_dir = os.path.join(current_dir, 'docker_official_images', args.kafka_version)
    shutil.copytree(os.path.join(current_dir, args.image_type), os.path.join
```

**How to spot**
When reviewing docker/common.py code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-19490: remove usages of distutils in docker scripts (#20178).

**Test**
Test modified in docker_build_test.py covering the corrected behaviour.

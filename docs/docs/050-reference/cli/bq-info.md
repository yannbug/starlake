---
sidebar_position: 20
title: bq-info / bq-freshness
---


## Synopsis

**starlake bq-info / bq-freshness [options]**

## Description


## Parameters

Parameter|Cardinality|Description
---|---|---
--write:`<value>`|*Optional*|One of Set(OVERWRITE, APPEND, ERROR_IF_EXISTS, IGNORE)
--connection:`<value>`|*Optional*|Connection to use
--database:`<value>`|*Optional*|database / project id
--external:`<value>`|*Optional*|Include external datasets defined in _config.sl.yml instead of using other parameters of this command ? Defaults to false
--tables:`<value>`|*Optional*|List of datasetName.tableName1,datasetName.tableName2 ...
--persist:`<value>`|*Optional*|Persist results ?


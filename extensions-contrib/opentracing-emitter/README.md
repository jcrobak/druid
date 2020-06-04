# OpenTracing Emitter

The OpenTracing emitter generates OpenTracing Spans for queries.


## Configuration

### Enabling

Load the plugin and enable the emitter in `common.runtime.properties`:

Load the plugin:

```
druid.extensions.loadList=[..., "opentracing-emitter"]

```

Enable the emitter:

```
druid.emitter=opentracing
```

### Forward trace id
The emitter will not generate a new traceid. It assumes that your application is sending
in a trace id as part of the query context, under the label "traceid".

### Customization
Currently none—only datadog export is supported.

## Testing

Run:

```
mvn package
tar -C /tmp -xf distribution/target/apache-druid-0.17.0-incubating-SNAPSHOT-bin.tar.gz
cd /tmp/apache-druid-0.17.0-incubating-SNAPSHOT
```

Edit `conf/druid/single-server/micro-quickstart/_common/common.runtime.properties` to enable
the emitter (see `Configuration` section above).

Start the quickstart:

```
bin/start-micro-quickstart
```

## Limitations

The spans generated by this emitter do not have parent span ids set. This means that
the traces may be able to properly visualize the fact that queries from the broker
fan out to the historicals and/or middle managers.
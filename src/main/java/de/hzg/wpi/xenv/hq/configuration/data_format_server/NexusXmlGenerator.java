package de.hzg.wpi.xenv.hq.configuration.data_format_server;

import de.hzg.wpi.xenv.hq.configuration.collections.DataSource;
import org.apache.commons.jxpath.JXPathContext;
import org.apache.commons.jxpath.JXPathNotFoundException;

import java.net.URI;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Callable;

/**
 * @author Igor Khokhriakov <igor.khokhriakov@hzg.de>
 * @since 2/20/19
 */
public class NexusXmlGenerator implements Callable<NxGroup> {
    private final List<DataSource> dataSourceList;
    private final NxGroup nexusXml;

    public NexusXmlGenerator(List<DataSource> dataSourceList, NxGroup nexusXml) {
        this.dataSourceList = dataSourceList;
        this.nexusXml = nexusXml;
    }

    @Override
    public NxGroup call() throws Exception {
        JXPathContext jxPathContext = JXPathContext.newContext(nexusXml);

        dataSourceList
                .forEach(dataSource -> {
                    NxPathParser.JxPath JXPath = new NxPathParser(
                            URI.create(dataSource.nxPath)).toJXPath();

                    NxGroup parent = getParentNxGroup(jxPathContext, JXPath);

                    processDataSource(dataSource, JXPath, jxPathContext);
                });
        return nexusXml;
    }

    private NxGroup getParentNxGroup(JXPathContext jxPathContext, NxPathParser.JxPath jxPath) {
        try {
            return (NxGroup) jxPathContext.getValue(
                    Optional.ofNullable(jxPath.getJxParentPath()).orElse(new NxPathParser.JxPath()).toString());
        } catch (JXPathNotFoundException e) {
            NxGroup result = new NxGroup();
            result.name = jxPath.getJxParentPath().getName();
            result.type = "NXcollection";

            getParentNxGroup(jxPathContext, jxPath.getJxParentPath()).groups.add(result);

            return result;
        }
    }

    private void processDataSource(DataSource dataSource, NxPathParser.JxPath jxPath, JXPathContext jxPathContext) {
        NxGroup parentGroup = (NxGroup) jxPathContext
                .getValue(jxPath.getJxParentPath().toString());
        switch (dataSource.type.toLowerCase()) {
            case "log":
                parentGroup.groups.add(
                        new DataSourceToNxLogConverter(jxPath.getName(), dataSource.dataType).call());
                break;
            case "spectrum":
                parentGroup.fields.add(new DataSourceToNxFieldWithDimensionsConverter(jxPath.getName(), dataSource.dataType)
                        .call());
                break;
            case "scalar":
                parentGroup.fields.add(new DataSourceToNxFieldConverter(jxPath.getName(), dataSource.dataType)
                        .call());
        }
    }
}

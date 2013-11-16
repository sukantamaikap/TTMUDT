package org.ttumdt.hbase.filters;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hbase.HBaseConfiguration;
import org.apache.hadoop.hbase.client.HTable;
import org.apache.hadoop.hbase.client.Result;
import org.apache.hadoop.hbase.client.ResultScanner;
import org.apache.hadoop.hbase.client.Scan;
import org.apache.hadoop.hbase.filter.*;
import org.apache.hadoop.hbase.mapreduce.MultiTableOutputFormat;
import org.apache.hadoop.hbase.util.Bytes;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.ttumdt.hbase.btshbase.ITrafficLogTable;

import java.io.IOException;
import java.util.*;

/**
 * Class for all generic filters tied to generic trafficLog
 */
public class BTSTrafficLogTableFilters implements ITrafficLogTable {

    private static Configuration conf = null;
    private final byte[] columnFamily = Bytes.toBytes(TRAFFIC_INFO_COLUMN_FAMILY);
    private final byte[] qualifierIMSI = Bytes.toBytes(COLUMN_IMSI);
    private final byte[] qualifierTimeStamp = Bytes.toBytes(COLUMN_TIMESTAMP);
    public final Logger LOG = Logger.getLogger(MultiTableOutputFormat.class);

    public BTSTrafficLogTableFilters () {
        LOG.setLevel(Level.ALL);
        conf = HBaseConfiguration.create();
        //conf.addResource();
    }

    /**
     * This filter will return list of IMSIs for a given btsId and ime interval
     * @param btsId : btsId for which the query has to run
     * @param startTime : start time for which the query has to run
     * @param endTime : end time for which the query has to run
     * @return returns IMSIs as set of Strings
     * @throws IOException
     */
    public Map<String, String> getInfoPerBTSID(String btsId, String date,
                                       String startTime, String endTime)
            throws IOException {
        //Set<String> imsis = new HashSet<String>();

        Map<String, String> imsiMap = new HashMap<String, String>();

        //ToDo : better exception handling
        HTable table = new HTable(conf, TRAFFIC_INFO_TABLE_NAME);
        Scan scan = new Scan();

        //scan.addColumn(columnFamily, qualifierIMSI);
        scan.addFamily(columnFamily);
        scan.setFilter(prepFilter(btsId, date, startTime, endTime));

        // filter to build where timestamp

        Result result = null;
        ResultScanner resultScanner = table.getScanner(scan);

        while ((result = resultScanner.next())!= null) {
            //byte[] obtainedColumn = result.getValue(columnFamily, qualifierIMSI);
            //imsis.add(Bytes.toString(obtainedColumn));

            byte[] obtainedColumnIMSI = result.getValue(columnFamily, qualifierIMSI);
            byte[] obtainedColumnTimeStamp = result.getValue(columnFamily,
                    qualifierTimeStamp);

            //imsis.add(Bytes.toString(obtainedColumnTimeStamp));

           imsiMap.put(Bytes.toString(obtainedColumnIMSI),
                    Bytes.toString(obtainedColumnTimeStamp));
        }

        resultScanner.close();
        //return  imsis;
        return imsiMap;
    }

    //ToDo : Figure out how valid is this filter code?? How comparison happens
    // with equal or grater than equal etc


    private Filter prepFilter (String btsId, String date,
                               String startTime, String endTime)
    {
        byte[] tableKey = Bytes.toBytes(KEY_TRAFFIC_INFO_TABLE_BTS_ID);
        byte[] timeStamp = Bytes.toBytes(COLUMN_TIMESTAMP);

        // filter to build -> where BTS_ID = <<btsId>> and Date = <<date>>
        RowFilter keyFilter = new RowFilter(CompareFilter.CompareOp.EQUAL,
                //new BinaryComparator(Bytes.toBytes(btsId+date)));
                new SubstringComparator(btsId+date));
        // filter to build -> where timeStamp >= startTime
        SingleColumnValueFilter singleColumnValueFilterStartTime =
                new SingleColumnValueFilter(columnFamily, timeStamp,
                        CompareFilter.CompareOp.GREATER_OR_EQUAL,Bytes.toBytes(startTime));

        // filter to build -> where timeStamp <= endTime
        SingleColumnValueFilter singleColumnValueFilterEndTime =
                new SingleColumnValueFilter(columnFamily, timeStamp,
                        CompareFilter.CompareOp.LESS_OR_EQUAL,Bytes.toBytes(endTime));

        FilterList filterList = new FilterList(FilterList.Operator.MUST_PASS_ALL, Arrays
                .asList((Filter)keyFilter,
                       singleColumnValueFilterStartTime, singleColumnValueFilterEndTime));
        return filterList;
    }


    public static void main(String[] args) throws IOException {
        BTSTrafficLogTableFilters flt = new BTSTrafficLogTableFilters();
        Map<String, String> imsis= flt.getInfoPerBTSID("AMCD000784", "26082013","060000","090000");
        System.out.println(imsis.toString());
        System.out.println("*************************IMSI count : " + imsis.size());
    }
}

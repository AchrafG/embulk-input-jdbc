package org.embulk.input.postgresql.getter;

import com.fasterxml.jackson.databind.JsonNode;
import org.embulk.config.ConfigException;
import org.embulk.input.jdbc.getter.AbstractTimestampColumnGetter;
import org.embulk.spi.PageBuilder;
import org.embulk.spi.time.Timestamp;
import org.embulk.spi.time.TimestampFormatter;
import org.embulk.spi.time.TimestampParseException;
import org.embulk.spi.time.TimestampParser;
import org.embulk.spi.type.Type;
import org.embulk.spi.type.Types;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class TimestampColumnGetter
        extends AbstractTimestampColumnGetter
{
    private static final String DEFAULT_FORMAT = "%Y-%m-%d %H:%M:%S";
    private final TimestampFormatter formatter;
    private final TimestampParser parser;
    private final String columnTypeName;

    public TimestampColumnGetter(PageBuilder to, Type toType, String columnTypeName, TimestampFormatter timestampFormatter, TimestampParser timestampParser)
    {
        super(to, toType, timestampFormatter);
        this.formatter = timestampFormatter;
        this.parser = timestampParser;
        this.columnTypeName = columnTypeName;
    }

    @Override
    protected void fetch(ResultSet from, int fromIndex) throws SQLException
    {
        java.sql.Timestamp timestamp = from.getTimestamp(fromIndex);
        if (timestamp != null) {
            value = Timestamp.ofEpochSecond(timestamp.getTime() / 1000, timestamp.getNanos());
        }
    }

    @Override
    protected Type getDefaultToType()
    {
        return Types.TIMESTAMP.withFormat(DEFAULT_FORMAT);
    }

    @Override
    public JsonNode encodeToJson()
    {
        return jsonNodeFactory.textNode(formatter.format(value));
    }

    @Override
    public void decodeFromJsonTo(PreparedStatement toStatement, int toIndex, JsonNode fromValue)
            throws SQLException
    {
        switch (columnTypeName) {
            case "timestamp":
                toStatement.setTimestamp(toIndex, java.sql.Timestamp.valueOf(fromValue.asText()));
                break;
            case "timestamptz":
                try {
                    // TODO parse error happens
                    Timestamp ts = parser.parse(fromValue.textValue());
                    // TODO
                    toStatement.setTimestamp(toIndex, java.sql.Timestamp.valueOf("TODO"));
                }
                catch (TimestampParseException ex) {
                    throw new ConfigException(ex);
                }
                break;
            default:
                toStatement.setTimestamp(toIndex, java.sql.Timestamp.valueOf(fromValue.asText()));
        }
    }
}

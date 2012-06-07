package ru.concerteza.util.db.springjdbc.parallel;

import java.util.List;
import java.util.Map;

/**
 * User: alexey
 * Date: 6/8/12
 */
public interface ResultsSaver {
    long save(List<Map<String, Object>> results);
}

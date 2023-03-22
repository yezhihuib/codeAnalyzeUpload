import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.DefaultTypedTuple;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

/**
 * @Description redis工具类
 * @ClassName RedisConfig
 * @Date 2017年9月10日
 */
@Service
public class RedisUtils {

    private static Logger log = Logger.getLogger(RedisUtils.class);

    /*** send tag interval time*/
    public final static int MAX_SEND_NUM = 125;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    /**
     * 制定緩存失效时间
     *
     * @param key
     * @param time
     * @return
     */
    public boolean expire(String key, long time) {
        try {
            if (time > 0) {
                redisTemplate.expire(key, time, TimeUnit.SECONDS);
            }
            return true;
        } catch (Exception e) {
            log.error("制定緩存失效時間异常!", e);
            return false;
        }
    }

    /**
     * 根据key获取过期时间
     *
     * @param key
     * @return
     */
    public long getExpire(String key) {
        return redisTemplate.getExpire(key);
    }

    /**
     * 判断key是否存在
     *
     * @param key
     * @return
     */
    public boolean hasKey(String key) {
        try {
            return redisTemplate.hasKey(key);
        } catch (Exception e) {
            log.error("根据key获取过期时间异常!", e);
            return false;
        }
    }

    /**
     * 删除缓存
     *
     * @param key 可以传一个值 或多个
     */
    @SuppressWarnings("unchecked")
    public void del(String... key) {
        if (key != null && key.length > 0) {
            if (key.length == 1) {
                redisTemplate.delete(key[0]);
            } else {
                redisTemplate.delete(CollectionUtils.arrayToList(key));
            }
        }
    }

    public void del(Collection<String> list) {
        if (list != null && list.size() > 0) {
            redisTemplate.delete(list);
        }
    }

    //============================String=============================

    /**
     * 模糊查询key
     *
     * @param @param key
     * @return Set<String> key的集合
     * @Title: keys
     */
    public Set<String> keys(String key) {
        return redisTemplate.keys(key);
    }

    public Set<String> scan(String key) {
        return redisTemplate.execute((RedisCallback<Set<String>>) connection1 -> {
            Set<String> keysTmp = new HashSet<>();
            Cursor<byte[]> cursor = connection1.scan(new ScanOptions.ScanOptionsBuilder().match("*" + key + "*").count(1000).build());
            while (cursor.hasNext()) {
                keysTmp.add(new String(cursor.next()));
            }
            return keysTmp;
        });
    }

    /**
     * 普通缓存获取
     *
     * @param key 键
     * @return 值
     */
    public <V> V get(String key) {
        return key == null ? null : (V) redisTemplate.opsForValue().get(key);
    }

    /**
     * 普通缓存多个获取
     *
     * @param key String类型，以","号分割的多个键
     * @return Object   值
     * @Title: mGet
     */
    public Object mGet(String key) {
        if (null == key || key.isEmpty()) {
            return null;
        }
        String[] keys = key.split(",");
        return mGet(keys);
    }

    /**
     * 普通缓存多个获取
     *
     * @param keys String数组类型的多个键
     * @return Object   值
     * @Title: mGet
     */
    public Object mGet(String[] keys) {
        if (null == keys || 0 == keys.length) {
            return null;
        }
        return mGet(Arrays.asList(keys));
    }

    /**
     * 普通缓存多个获取
     *
     * @param keys Collection类型的多个键
     * @return Object   值
     * @Title: mGet
     */
    public <V> List<V> mGet(Collection<String> keys) {
        if (null == keys) {
            return null;
        }
        List<Object> values = redisTemplate.opsForValue().multiGet(keys);
        if (values == null) {
            return null;
        }
        return values.stream().filter(Objects::nonNull).map(v -> (V) v).collect(Collectors.toList());
    }

    /**
     * 普通缓存放入
     *
     * @param key   键
     * @param value 值
     * @return true成功 false失败
     */
    public boolean set(String key, Object value) {
        try {
            redisTemplate.opsForValue().set(key, value);
            return true;
        } catch (Exception e) {
            log.error("set缓存放入异常!", e);
            return false;
        }

    }

    /**
     * 普通缓存放入并设置时间
     *
     * @param key   键
     * @param value 值
     * @param time  时间(秒) time要大于0 如果time小于等于0 将设置无限期
     * @return true成功 false 失败
     */
    public boolean set(String key, Object value, long time) {
        try {
            if (time > 0) {
                redisTemplate.opsForValue().set(key, value, time, TimeUnit.SECONDS);
            } else {
                set(key, value);
            }
            return true;
        } catch (Exception e) {
            log.error("set缓存放入并设置时间异常!", e);
            return false;
        }
    }

    /**
     * 普通缓存多个放入
     *
     * @param @param map 多个键值对
     * @return true成功 false 失败
     */
    public boolean mSet(Map<String, Object> map) {
        try {
            redisTemplate.opsForValue().multiSet(map);
            return true;
        } catch (Exception e) {
            log.error("set缓存多个放入异常!", e);
            return false;
        }
    }

    /**
     * 递增
     *
     * @param key   键
     * @param delta 要增加几(大于0)
     * @return
     */
    public long incr(String key, long delta) {
        if (delta < 0) {
            throw new RuntimeException("递增因子必须大于0");
        }
        return redisTemplate.opsForValue().increment(key, delta);
    }

    /**
     * 递减
     *
     * @param key   键
     * @param delta 要减少几(小于0)
     * @return
     */
    public long decr(String key, long delta) {
        if (delta < 0) {
            throw new RuntimeException("递减因子必须大于0");
        }
        return redisTemplate.opsForValue().increment(key, -delta);
    }

    //================================Map=================================

    /**
     * HashGet
     *
     * @param key  键 不能为null
     * @param item 项 不能为null
     * @return 值
     */
    public Object hget(String key, String item) {
        return redisTemplate.opsForHash().get(key, item);
    }

    /**
     * 获取hashKey对应的所有键值
     *
     * @param key 键
     * @return 对应的多个键值
     */
    public Map<Object, Object> hmget(String key) {
        return redisTemplate.opsForHash().entries(key);
    }

    /**
     * HashSet
     *
     * @param key 键
     * @param map 对应多个键值
     * @return true 成功 false 失败
     */
    public boolean hmset(String key, Map<String, Object> map) {
        try {
            redisTemplate.opsForHash().putAll(key, map);
            return true;
        } catch (Exception e) {
            log.error("HashSet异常!", e);
            return false;
        }
    }

    /**
     * HashSet 并设置时间
     *
     * @param key  键
     * @param map  对应多个键值
     * @param time 时间(秒)
     * @return true成功 false失败
     */
    public boolean hmset(String key, Map<String, Object> map, long time) {
        try {
            redisTemplate.opsForHash().putAll(key, map);
            if (time > 0) {
                expire(key, time);
            }
            return true;
        } catch (Exception e) {
            log.error("HashSet并设置时间异常!", e);
            return false;
        }
    }

    /**
     * 向一张hash表中放入数据,如果不存在将创建
     *
     * @param key   键
     * @param item  项
     * @param value 值
     * @return true 成功 false失败
     */
    public boolean hset(String key, String item, Object value) {
        try {
            redisTemplate.opsForHash().put(key, item, value);
            return true;
        } catch (Exception e) {
            log.error("hash表中放入数据,如果不存在将创建异常!", e);
            return false;
        }
    }

    /**
     * 向一张hash表中放入数据,如果不存在将创建
     *
     * @param key   键
     * @param item  项
     * @param value 值
     * @param time  时间(秒)  注意:如果已存在的hash表有时间,这里将会替换原有的时间
     * @return true 成功 false失败
     */
    public boolean hset(String key, String item, Object value, long time) {
        try {
            redisTemplate.opsForHash().put(key, item, value);
            if (time > 0) {
                expire(key, time);
            }
            return true;
        } catch (Exception e) {
            log.error("hash表中放入数据,如果不存在将创建异常!", e);
            return false;
        }
    }

    /**
     * 删除hash表中的值
     *
     * @param key  键 不能为null
     * @param item 项 可以使多个 不能为null
     */
    public void hdel(String key, Object... item) {
        redisTemplate.opsForHash().delete(key, item);
    }

    /**
     * 判断hash表中是否有该项的值
     *
     * @param key  键 不能为null
     * @param item 项 不能为null
     * @return true 存在 false不存在
     */
    public boolean hHasKey(String key, String item) {
        return redisTemplate.opsForHash().hasKey(key, item);
    }

    /**
     * hash递增 如果不存在,就会创建一个 并把新增后的值返回
     *
     * @param key  键
     * @param item 项
     * @param by   要增加几(大于0)
     * @return
     */
    public double hincr(String key, String item, double by) {
        return redisTemplate.opsForHash().increment(key, item, by);
    }

    /**
     * hash递减
     *
     * @param key  键
     * @param item 项
     * @param by   要减少记(小于0)
     * @return
     */
    public double hdecr(String key, String item, double by) {
        return redisTemplate.opsForHash().increment(key, item, -by);
    }

    //============================set=============================

    /**
     * 根据key获取Set中的所有值
     *
     * @param key 键
     * @return
     */
    public Set<Object> sGet(String key) {
        try {
            return redisTemplate.opsForSet().members(key);
        } catch (Exception e) {
            log.error("根据key获取Set中的所有值异常!", e);
            return null;
        }
    }

    /**
     * 根据value从一个set中查询,是否存在
     *
     * @param key   键
     * @param value 值
     * @return true 存在 false不存在
     */
    public boolean sHasKey(String key, Object value) {
        try {
            return redisTemplate.opsForSet().isMember(key, value);
        } catch (Exception e) {
            log.error("根据value从一个set中查询,是否存在异常!", e);
            return false;
        }
    }

    /**
     * 将数据放入set缓存
     *
     * @param key    键
     * @param values 值 可以是多个
     * @return 成功个数
     */
    public long sSet(String key, Object... values) {
        try {
            return redisTemplate.opsForSet().add(key, values);
        } catch (Exception e) {
            log.error("将数据放入set缓存异常!", e);
            return 0;
        }
    }

    /**
     * 将数据放入set缓存
     *
     * @param key    键
     * @param values 值 可以是多个
     * @return 成功个数
     */
    public <V> void sSet(String key, Collection<V> values) {
        try {
            values.forEach(value -> {
                redisTemplate.opsForSet().add(key, value);
            });
        } catch (Exception e) {
            log.error("将数据放入set缓存异常!", e);
        }
    }

    /**
     * 将set数据放入缓存
     *
     * @param key    键
     * @param time   时间(秒)
     * @param values 值 可以是多个
     * @return 成功个数
     */
    public long sSetAndTime(String key, long time, Object... values) {
        try {
            Long count = redisTemplate.opsForSet().add(key, values);
            if (time > 0) {
                expire(key, time);
            }
            return count;
        } catch (Exception e) {
            log.error("将set数据放入缓存异常!", e);
            return 0;
        }
    }

    /**
     * 获取set缓存的长度
     *
     * @param key 键
     * @return
     */
    public long sGetSetSize(String key) {
        try {
            return redisTemplate.opsForSet().size(key);
        } catch (Exception e) {
            log.error("获取set缓存的长度异常!", e);
            return 0;
        }
    }

    /**
     * 移除值为value的
     *
     * @param key    键
     * @param values 值 可以是多个
     * @return 移除的个数
     */
    public long setRemove(String key, Object... values) {
        try {
            Long count = redisTemplate.opsForSet().remove(key, values);
            return count;
        } catch (Exception e) {
            log.error("移除值为value异常!", e);
            return 0;
        }
    }
    //===============================list=================================

    /**
     * 获取list缓存的内容
     *
     * @param key   键
     * @param start 开始
     * @param end   结束  0 到 -1代表所有值
     * @return
     */
    public List<Object> lGet(String key, long start, long end) {
        try {
            return redisTemplate.opsForList().range(key, start, end);
        } catch (Exception e) {
            log.error("获取list缓存的内容异常!", e);
            return null;
        }
    }

    /**
     * 获取list缓存的长度
     *
     * @param key 键
     * @return
     */
    public long lGetListSize(String key) {
        try {
            return redisTemplate.opsForList().size(key);
        } catch (Exception e) {
            log.error("获取list缓存的长度异常!", e);
            return 0;
        }
    }

    /**
     * 通过索引 获取list中的值
     *
     * @param key   键
     * @param index 索引  index>=0时， 0 表头，1 第二个元素，依次类推；index<0时，-1，表尾，-2倒数第二个元素，依次类推
     * @return
     */
    public Object lGetIndex(String key, long index) {
        try {
            return redisTemplate.opsForList().index(key, index);
        } catch (Exception e) {
            log.error("通过索引获取list中的值异常!", e);
            return null;
        }
    }

    /**
     * 将list放入缓存
     *
     * @param key   键
     * @param value 值
     * @param value 时间(秒)
     * @return
     */
    public boolean lSet(String key, Object value) {
        try {
            redisTemplate.opsForList().rightPush(key, value);
            return true;
        } catch (Exception e) {
            log.error("list放入缓存异常!", e);
            return false;
        }
    }

    /**
     * 将list放入缓存
     *
     * @param key   键
     * @param value 值
     * @param time  时间(秒)
     * @return
     */
    public boolean lSet(String key, Object value, long time) {
        try {
            redisTemplate.opsForList().rightPush(key, value);
            if (time > 0) {
                expire(key, time);
            }
            return true;
        } catch (Exception e) {
            log.error("list放入缓存异常!", e);
            return false;
        }
    }

    /**
     * 将list放入缓存
     *
     * @param key   键
     * @param value 值
     * @return
     */
    public boolean lSet(String key, List<Object> value) {
        try {
            redisTemplate.opsForList().rightPushAll(key, value);
            return true;
        } catch (Exception e) {
            log.error("list放入缓存异常!", e);
            return false;
        }
    }

    /**
     * 将list放入缓存
     *
     * @param key   键
     * @param value 值
     * @param time  时间(秒)
     * @return
     */
    public boolean lSet(String key, List<Object> value, long time) {
        try {
            redisTemplate.opsForList().rightPushAll(key, value);
            if (time > 0) {
                expire(key, time);
            }
            return true;
        } catch (Exception e) {
            log.error("list放入缓存异常!", e);
            return false;
        }
    }

    /**
     * 根据索引修改list中的某条数据
     *
     * @param key   键
     * @param index 索引
     * @param value 值
     * @return
     */
    public boolean lUpdateIndex(String key, long index, Object value) {
        try {
            redisTemplate.opsForList().set(key, index, value);
            return true;
        } catch (Exception e) {
            log.error("根据索引修改list中的某条数据异常!", e);
            return false;
        }
    }

    /**
     * 移除N个值为value
     *
     * @param key   键
     * @param count 移除多少个
     * @param value 值
     * @return 移除的个数
     */
    public long lRemove(String key, long count, Object value) {
        try {
            Long remove = redisTemplate.opsForList().remove(key, count, value);
            return remove;
        } catch (Exception e) {
            log.error("移除N个值为value异常!", e);
            return 0;
        }
    }

    public <V> void lRightPush(String key, V value) {
        redisTemplate.opsForList().rightPush(key, value);
    }

    public <V> void lRightPush(String key, Collection<V> values) {
        values.forEach(value -> {
            lRightPush(key, value);
        });
    }

    public <V> V lLeftPop(String key) {
        return (V) redisTemplate.opsForList().leftPop(key);
    }

    public void lRemoveAll(String key) {
        redisTemplate.opsForList().trim(key, 1, 0);
    }

    public void lRemoveTopX(String key, Integer x) {
        if (x < 1) {
            throw new RuntimeException("x can not below 1");
        }
        redisTemplate.opsForList().trim(key, x, -1);
    }

    public <V> V lTop(String key) {
        List<Object> list = redisTemplate.opsForList().range(key, 0, 0);
        if (list == null || list.isEmpty()) {
            return null;
        }
        return (V) (list.get(0));
    }

    public Long lSize(String key) {
        return redisTemplate.opsForList().size(key);
    }

    public RedisUtils() {
        super();
    }

    public <V> List<V> lRange(String key, long start, long end) {
        List<Object> rangeResult = redisTemplate.opsForList().range(key, start, end);
        if (rangeResult == null) {
            return null;
        }
        return rangeResult.stream().map(v -> (V) v).collect(Collectors.toList());
    }

    public void zAdd(String key, Object value, double score) {
        redisTemplate.opsForZSet().add(key, value, score);
    }

    public Long zRemove(String key, Object value) {
        return redisTemplate.opsForZSet().remove(key, value);
    }

    public <V> Set<V> zRange(String key, long start, long end) {
        Set<Object> rangeResult = redisTemplate.opsForZSet().range(key, start, end);
        if (rangeResult == null) {
            return null;
        }
        return rangeResult.stream().map(v -> (V) v).collect(Collectors.toSet());
    }

    public <V> V zTop(String key) {
        Set<V> rangeResult = zRange(key, 0, 0);
        if (rangeResult == null) {
            return null;
        }
        return rangeResult.stream().findFirst().orElse(null);
    }

    public <V> DefaultTypedTuple<V> zTopWithScores(String key) {
        Set<ZSetOperations.TypedTuple<Object>> rangeResult = redisTemplate.opsForZSet().rangeWithScores(key, 0, 0);
        if (rangeResult == null) {
            return null;
        }
        return rangeResult.stream().map(typedTuple -> new DefaultTypedTuple<V>((V) typedTuple.getValue(), typedTuple.getScore())).findFirst().orElse(null);
    }

    public Long zSize(String key) {
        return redisTemplate.opsForZSet().size(key);
    }

    public List<String> getKeysWithPrefix(String tagPrefix, Collection<String> collection) {
        return collection.stream().map(str -> tagPrefix + str).collect(Collectors.toList());
    }
}

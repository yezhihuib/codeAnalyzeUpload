import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import javax.transaction.Transactional;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.util.ObjectUtils;

import com.pinpoint.db.model.BusAnchor;
import com.pinpoint.db.model.BusAnchorConfig;
import com.pinpoint.db.model.BusAnchorGroup;
import com.pinpoint.db.model.BusAnchorSwitchArea;
import com.pinpoint.db.model.BusAnchorSwitchAreaNeighbor;
import com.pinpoint.db.model.BusAnchorVersion;
import com.pinpoint.db.model.BusMap;
import com.pinpoint.db.model.BusSwitchLine;
import com.pinpoint.db.model.SysConfig;
import com.pinpoint.db.repository.IAnchorDao;
import com.pinpoint.db.repository.IAnchorGroupDao;
import com.pinpoint.db.repository.ISwitchAreaDao;
import com.pinpoint.global.AnchorVersionVariable;
import com.pinpoint.global.GlobalVariables;
import com.pinpoint.parser.toa.ModuleAnchorMap;
import com.pinpoint.redis.RedisUtils;
import com.pinpoint.service.BaseService;
import com.pinpoint.service.IConfigService;
import com.pinpoint.service.IMapService;
import com.pinpoint.service.ISwitchAreaService;
import com.pinpoint.utils.SpringUtil;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@Transactional(rollbackOn = Exception.class)
public class SwitchAreaServiceImpl extends BaseService implements ISwitchAreaService {

    @Autowired
    private ISwitchAreaDao switchAreaDao;

    @Autowired
    private IAnchorDao anchorDao;

    @Autowired
    private IAnchorGroupDao groupDao;

    @Autowired
    private IConfigService configService;

    @Autowired
    private IMapService mapService;

    @Autowired
    private RedisUtils redisUtils;

    protected Map<String, Object> areaInfoMap = new HashMap<>();

    private final String RUB = "yes";

    private Integer GraphicsType = null;

    @Override
    public Page<BusAnchorSwitchArea> getAreasByPage(String areaId, Integer pageNo, Integer pageSize) {
        Pageable pageable = this.buildPageRequest(pageNo, pageSize, Sort.Direction.DESC, "id");
        //规格定义
        Specification<BusAnchorSwitchArea> specification = new Specification<BusAnchorSwitchArea>() {
            /**
             *
             */
            private static final long serialVersionUID = 1L;

            /**
             * 构造断言
             * @param root 实体对象引用
             * @param query 规则查询对象
             * @param cb 规则构建对象
             * @return 断言
             */
            @Override
            public Predicate toPredicate(Root<BusAnchorSwitchArea> root, CriteriaQuery<?> query, CriteriaBuilder cb) {
                List<Predicate> predicates = new ArrayList<>(); //所有的断言
                if (StringUtils.isNotBlank(areaId)) { //添加断言
                    Predicate likeAreaId = cb.like(root.get("areaId").as(String.class), areaId + "%");
                    predicates.add(likeAreaId);
                }
                return cb.and(predicates.toArray(new Predicate[0]));
            }
        };
        Page<BusAnchorSwitchArea> pages = switchAreaDao.findAll(specification, pageable);

//        Iterable<BusAnchorSwitchArea> areaIter =  switchAreaDao.findAll();
//        List<BusAnchorSwitchArea> areaList = new ArrayList<BusAnchorSwitchArea>();
//        for(BusAnchorSwitchArea area : areaIter){
//        	areaList.add(area);
//        }
//        AreaDefinedGenerateUtils util = new AreaDefinedGenerateUtils(areaList,10);
//        util.setSeq();
        return pages;
    }

    @Override
    public int addArea(String areaId, Long areaNo, Long mapId, Long minZ, Long maxZ, String color, String pointsStr, Integer type, Integer leaveRSSIEnable, Integer bsAreaType, Integer graphicsType, Integer borderType, Integer disableWarning) {
        if (borderType.equals(1) && (StringUtils.isBlank(areaId) || StringUtils.isBlank(color) || StringUtils.isBlank(pointsStr))) {
            return GlobalVariables.NULL_PARAM;
        }
        BusAnchorSwitchArea existsArea = switchAreaDao.getAreaByAreaId(areaId);
        if (existsArea != null) {
            return GlobalVariables.DATA_EXISTS_ERROR;
        }
        existsArea = switchAreaDao.getAreaByAreaNo(areaNo);
        if (existsArea != null) {
            return GlobalVariables.DATA_EXISTS_ERROR;
        }

        BusAnchorSwitchArea switchArea = new BusAnchorSwitchArea();
        switchArea.setAreaId(areaId);
        switchArea.setShowColor(color);
        switchArea.setAreaNo(areaNo);
        switchArea.setMinZ(minZ);
        switchArea.setMaxZ(maxZ);
        switchArea.setSort(1);
        switchArea.setType(type);
        switchArea.setMapId(mapId);
        switchArea.setLeaveRSSIEnable(leaveRSSIEnable);
        switchArea.setBsAreaType(bsAreaType);
        switchArea.setGraphicsType(graphicsType);
        switchArea.setBorderType(borderType);
        switchArea.setDisableWarning(disableWarning);
        if (borderType.equals(1)) {
            switchArea.setMapId(mapId);
            switchArea.setLines(formatLine(switchArea, pointsStr));
        }
        switchArea = switchAreaDao.save(switchArea);
        if (switchArea.getId() > 0) {
            if (borderType.equals(1)) {
                ModuleAnchorMap maMap = SpringUtil.getBean(ModuleAnchorMap.class);
                maMap.addSwitchArea(switchArea.getAreaNo());
                this.areaInfoMap.put("anchor", switchArea);
                this.areaInfoMap.put("lines", switchArea.getLines());
            }
            return GlobalVariables.OPERATOR_SUCCESS;
        } else {
            return GlobalVariables.OPERATOR_FAILED;
        }
    }

    @Override
    public int delArea(Iterable<Long> ids) {
        Iterable<BusAnchorSwitchArea> areas = switchAreaDao.findAllById(ids);
        areas.forEach(area -> {
            switchAreaDao.deleteNeighborAsNeighborById(area.getId());
        });
        if (null != areas) {
            switchAreaDao.deleteAll(areas);
            ModuleAnchorMap maMap = (ModuleAnchorMap) SpringUtil.getBean(ModuleAnchorMap.class);
            for (BusAnchorSwitchArea area : areas) {
                maMap.removeSwitchArea(area.getAreaNo());
            }
            return GlobalVariables.OPERATOR_SUCCESS;
        }
        return GlobalVariables.OPERATOR_FAILED;
    }

    @Override
    public BusAnchorSwitchArea getArea(Long areaNo) {
        Optional<BusAnchorSwitchArea> optArea = switchAreaDao.findById(areaNo);
        return optArea.orElse(null);
    }

    @Override
    public Set<BusSwitchLine> getLines(Long areaNo) {
        Optional<BusAnchorSwitchArea> optArea = switchAreaDao.findById(areaNo);
        if (optArea.isPresent()) {
            BusAnchorSwitchArea switchArea = optArea.get();
            this.GraphicsType = switchArea.getGraphicsType();
            return switchArea.getLines();
        }
        return null;
    }

    @Override
    public Set<BusAnchor> getAreaAnchors(Long areaNo) {
        Optional<BusAnchorSwitchArea> optArea = switchAreaDao.findById(areaNo);
        if (optArea.isPresent()) {
            BusAnchorSwitchArea area = optArea.get();
            return area.getAnchors();
        }
        return null;
    }

    @Override
    public int addNeighbor(Long areaNo, Long neighborNo, String pointsStr) {
        Optional<BusAnchorSwitchArea> optArea = switchAreaDao.findById(areaNo);

        if (!optArea.isPresent()) {
            return GlobalVariables.OPERATOR_NULL_RESULT;
        }
        BusAnchorSwitchArea area = optArea.get();
        Optional<BusAnchorSwitchArea> optNeighor = switchAreaDao.findById(neighborNo);

        if (!optNeighor.isPresent()) {
            return GlobalVariables.OPERATOR_NULL_RESULT;
        }
        BusAnchorSwitchArea neighorArea = optNeighor.get();
        Set<BusAnchorSwitchAreaNeighbor> neighbors = area.getNeighbors();
        for (BusAnchorSwitchAreaNeighbor neighbor : neighbors) {
            if (neighbor.getNeighborArea().getId().longValue() == neighorArea.getId().longValue()) {
                return GlobalVariables.DATA_EXISTS_ERROR;
            }
        }

        if (StringUtils.isBlank(pointsStr)) {
            return GlobalVariables.NULL_PARAM;
        }
        String[] pointArr = pointsStr.split(" ");
        String start = pointArr[0];
        String end = pointArr[1];

        String[] startArr = start.split(",");
        String[] endArr = end.split(",");

        BusAnchorSwitchAreaNeighbor newNeighbor = new BusAnchorSwitchAreaNeighbor();
        newNeighbor.setNeighborArea(neighorArea);
        newNeighbor.setSwitchArea(area);
        newNeighbor.setSepStartX(NumberUtils.toLong(startArr[0]));
        newNeighbor.setSepStartY(NumberUtils.toLong(startArr[1]));
        newNeighbor.setSepEndX(NumberUtils.toLong(endArr[0]));
        newNeighbor.setSepEndY(NumberUtils.toLong(endArr[1]));
        neighbors.add(newNeighbor);
        switchAreaDao.save(area);
        return GlobalVariables.OPERATOR_SUCCESS;
    }

    @Override
    public int removeNeighbor(Long areaNo, Iterable<Long> ids) {
        Optional<BusAnchorSwitchArea> optArea = switchAreaDao.findById(areaNo);

        if (!optArea.isPresent()) {
            return GlobalVariables.OPERATOR_NULL_RESULT;
        }
        BusAnchorSwitchArea area = optArea.get();
        Iterable<BusAnchorSwitchArea> removeNeighbors = switchAreaDao.findAllById(ids);
        Set<BusAnchorSwitchAreaNeighbor> neighbors = area.getNeighbors();
        List<BusAnchorSwitchAreaNeighbor> removeNeighborList = new ArrayList<BusAnchorSwitchAreaNeighbor>();
        for (BusAnchorSwitchAreaNeighbor neighbor : neighbors) {
            for (BusAnchorSwitchArea removeNeighbor : removeNeighbors) {
                if (neighbor.getNeighborArea().getId().equals(removeNeighbor.getId())) {
                    removeNeighborList.add(neighbor);
                    break;
                }
            }
        }
        if (!removeNeighborList.isEmpty()) {
            for (BusAnchorSwitchAreaNeighbor _rmove : removeNeighborList) {
                // neighbors.remove(_rmove);
                switchAreaDao.deleteNeighborById(_rmove.getId());
            }
            //switchAreaDao.save(area);
        }

        return GlobalVariables.OPERATOR_SUCCESS;
    }

    @Override
    public int addAnchor(Long areaId, Long anchorId) {
        Optional<BusAnchorSwitchArea> optArea = switchAreaDao.findById(areaId);

        if (optArea.isPresent()) {
            BusAnchorSwitchArea area = optArea.get();
            Optional<BusAnchor> anchorOpt = anchorDao.findById(anchorId);

            if (anchorOpt.isPresent()) {
                BusAnchor anchor = anchorOpt.get();
                Set<BusAnchor> anchors = area.getAnchors();
                if (null == anchors) {
                    anchors = new HashSet<BusAnchor>();
                    area.setAnchors(anchors);
                }
                boolean isContains = false;
                for (BusAnchor _anchor : anchors) {
                    if (_anchor.getId() == anchor.getId()) {
                        isContains = true;
                        break;
                    }
                }
                if (!isContains) {
                    anchors.add(anchor);
                    switchAreaDao.save(area);
                    ModuleAnchorMap maMap = (ModuleAnchorMap) SpringUtil.getBean(ModuleAnchorMap.class);
                    maMap.addSwitchAreaAnchor(area.getAreaNo(), anchor.getAnchorId());
                }
                return GlobalVariables.OPERATOR_SUCCESS;
            } else {
                return GlobalVariables.OPERATOR_NULL_RESULT;
            }
        } else {
            return GlobalVariables.OPERATOR_NULL_RESULT;
        }
    }

    @Override
    public int removeAnchor(Long areaId, Long anchorId) {
        Optional<BusAnchorSwitchArea> areaOpt = switchAreaDao.findById(areaId);

        if (areaOpt.isPresent()) {
            BusAnchorSwitchArea area = areaOpt.get();
            Optional<BusAnchor> anchorOpt = anchorDao.findById(anchorId);

            if (anchorOpt.isPresent()) {
                BusAnchor anchor = anchorOpt.get();
                Set<BusAnchor> anchors = area.getAnchors();
                if (null == anchors) {
                    anchors = new HashSet<BusAnchor>();
                    area.setAnchors(anchors);
                }
                ModuleAnchorMap maMap = (ModuleAnchorMap) SpringUtil.getBean(ModuleAnchorMap.class);
                for (BusAnchor _anchor : anchors) {
                    if (_anchor.getId().equals(anchor.getId())) {
                        anchors.remove(_anchor);
                        maMap.removeSwitchAreaAnchor(area.getAreaNo(), _anchor.getAnchorId());
                        break;
                    }
                }
                switchAreaDao.save(area);
                return GlobalVariables.OPERATOR_SUCCESS;
            } else {
                return GlobalVariables.OPERATOR_NULL_RESULT;
            }
        } else {
            return GlobalVariables.OPERATOR_NULL_RESULT;
        }
    }

    @Override
    public List<BusAnchorSwitchArea> getOtherSwitchAreaForSelect(Long araeId) {
        //规格定义
        Specification<BusAnchorSwitchArea> specification = new Specification<BusAnchorSwitchArea>() {
            /**
             *
             */
            private static final long serialVersionUID = 1L;

            /**
             * 构造断言
             * @param root 实体对象引用
             * @param query 规则查询对象
             * @param cb 规则构建对象
             * @return 断言
             */
            @Override
            public Predicate toPredicate(Root<BusAnchorSwitchArea> root, CriteriaQuery<?> query, CriteriaBuilder cb) {
                List<Predicate> predicates = new ArrayList<>(); //所有的断言
                if (null != araeId) { //添加断言
                    Predicate notEqAreaId = cb.notEqual(root.get("id").as(Long.class), araeId);
                    predicates.add(notEqAreaId);
                }
                return cb.and(predicates.toArray(new Predicate[0]));
            }
        };
        return switchAreaDao.findAll(specification);
    }

    @Override
    public List<BusAnchorSwitchArea> getSelectedNeighbor(Long areaId) {
        List<BusAnchorSwitchArea> _neighbors = new ArrayList<BusAnchorSwitchArea>();
        Optional<BusAnchorSwitchArea> areaOpt = switchAreaDao.findById(areaId);

        if (areaOpt.isPresent()) {
            BusAnchorSwitchArea area = areaOpt.get();
            Set<BusAnchorSwitchAreaNeighbor> neighbors = area.getNeighbors();
            for (BusAnchorSwitchAreaNeighbor neighbor : neighbors) {
                _neighbors.add(neighbor.getNeighborArea());
            }
        }
        return _neighbors;
    }

    @Override
    public BusAnchorSwitchAreaNeighbor getNeighborById(Long areaId, Long neighorNo) {
        Optional<BusAnchorSwitchArea> areaOpt = switchAreaDao.findById(areaId);
        if (!areaOpt.isPresent()) {
            return null;
        }
        BusAnchorSwitchArea area = areaOpt.get();
        BusAnchorSwitchAreaNeighbor neighbor = null;


        Set<BusAnchorSwitchAreaNeighbor> neighbors = area.getNeighbors();
        for (BusAnchorSwitchAreaNeighbor _neighbor : neighbors) {
            if (_neighbor.getNeighborArea().getId().longValue() == neighorNo.longValue()) {
                neighbor = _neighbor;
                break;
            }
        }
        return neighbor;
    }

    @Override
    public int updateArea(Long id, String areaId, Long areaNo, Long minZ, Long maxZ, String color, Integer type, Integer leaveRSSIEnable, Integer bsAreaType, Integer disableWarning) {
        if (StringUtils.isBlank(areaId) || StringUtils.isBlank(color)) {
            return GlobalVariables.NULL_PARAM;
        }
        BusAnchorSwitchArea existsArea = switchAreaDao.getAreaByAreaId(areaId);
        if (existsArea != null) {
            if (existsArea.getId().longValue() != id.longValue()) {
                return GlobalVariables.DATA_EXISTS_ERROR;
            }

        }
        existsArea = switchAreaDao.getAreaByAreaNo(areaNo);
        if (existsArea != null) {
            if (existsArea.getId().longValue() != id.longValue()) {
                return GlobalVariables.DATA_EXISTS_ERROR;
            }
        }
        Optional<BusAnchorSwitchArea> areaOpt = switchAreaDao.findById(id);

        if (areaOpt.isPresent()) {
            BusAnchorSwitchArea area = areaOpt.get();
            area.setAreaId(areaId);
            area.setAreaNo(areaNo);
            area.setMinZ(minZ);
            area.setMaxZ(maxZ);
            area.setShowColor(color);
            area.setType(type);
            area.setLeaveRSSIEnable(leaveRSSIEnable);
            area.setBsAreaType(bsAreaType);
            area.setDisableWarning(disableWarning);
            switchAreaDao.save(area);
            return GlobalVariables.OPERATOR_SUCCESS;
        } else {
            return GlobalVariables.OPERATOR_NULL_RESULT;
        }
    }

    @Override
    public int addAnchors(Long areaId, String anchorIds, String groupIds) {
        if (null == areaId) {
            return GlobalVariables.NULL_PARAM;
        }
        Optional<BusAnchorSwitchArea> areaOpt = switchAreaDao.findById(areaId);
        if (areaOpt.isPresent()) {
            BusAnchorSwitchArea area = areaOpt.get();
            if (StringUtils.isNoneBlank(anchorIds)) {
                List<Long> idList = this.idTypeChange(anchorIds);
                Iterable<BusAnchor> anchors = anchorDao.findAllById(idList);
                if (anchors != null) {
                    Set<BusAnchor> anchorSet = new HashSet<>();
                    if (Objects.nonNull(groupIds) && !groupIds.isEmpty()) {
                        List<Long> groupList = this.idTypeChange(groupIds);
                        Iterable<BusAnchorGroup> groups = groupDao.findAllById(groupList);
                        Set<BusAnchorGroup> groupSet = new HashSet<>();
                        groups.forEach(group -> groupSet.add(group));
                        area.setAnchorGroups(groupSet);
                        Set<BusAnchor> busAnchors = new HashSet<>();
                        anchors.forEach(anchor -> busAnchors.add(anchor));
                        anchorSet.addAll(busAnchors);
                    } else {
                        anchorSet = area.getAnchors().isEmpty() ? new HashSet<>() : area.getAnchors();
                        for (BusAnchor anchor : anchors) {
                            if (anchorSet.isEmpty() || !anchorSet.contains(anchor)) {
                                anchorSet.add(anchor);
                            }
                        }
                        area.setAnchorGroups(null);
                    }
                    area.setAnchors(anchorSet);
                    switchAreaDao.save(area);
                    return GlobalVariables.OPERATOR_SUCCESS;
                } else {
                	area.setAnchors(null);
                	if(StringUtils.isEmpty(groupIds)) {
                		area.setAnchorGroups(null);
                	}
                	switchAreaDao.save(area);
                    return GlobalVariables.OPERATOR_SUCCESS;
                }
            } else {
            	area.setAnchors(null);
            	if(StringUtils.isEmpty(groupIds)) {
            		area.setAnchorGroups(null);
            	}
            	switchAreaDao.save(area);
                return GlobalVariables.OPERATOR_NULL_RESULT;
            }
        } else {
            return GlobalVariables.OPERATOR_NULL_RESULT;
        }
    }

    @Override
    public Iterable<BusAnchorSwitchArea> getAllArea(List<Long> ids) {
        if (Objects.isNull(ids) || ids.isEmpty()) {
            return switchAreaDao.findAll();
        } else {
            return switchAreaDao.findAllById(ids);
        }
    }

    @Override
    public int updataAreaRegion(Long id, String pointsStr, String isRub, Integer graphicsType) {
        if (id == null || StringUtils.isBlank(pointsStr)) {
            return GlobalVariables.NULL_PARAM;
        }
        Optional<BusAnchorSwitchArea> areaOpt = switchAreaDao.findById(id);
        if (areaOpt.isPresent()) {
            BusAnchorSwitchArea area = areaOpt.get();
            area.setGraphicsType(graphicsType);
            for (BusSwitchLine line : area.getLines()) {
                switchAreaDao.deleteAreaLine(line.getId());
            }
            area.getLines().clear();
            area.setLines(formatLine(area, pointsStr));
            switchAreaDao.save(area);
            if (area.getId() > 0) {
                this.areaInfoMap.put("anchor", area);
                this.areaInfoMap.put("lines", area.getLines());
                if (this.RUB.equals(isRub)) {
                    Set<BusAnchor> busAnchors = getAreaAnchors(area.getId());
                    while (busAnchors.iterator().hasNext()) {
                        BusAnchor anchor = busAnchors.iterator().next();
                        this.removeAnchor(area.getId(), anchor.getId());
                    }
                }
            }
            return GlobalVariables.OPERATOR_SUCCESS;
        } else {
            return GlobalVariables.OPERATOR_NULL_RESULT;
        }
    }

    @Override
    public Set<BusAnchor> getSwitchAreaAnchors(Long areaNo) {
        return switchAreaDao.getAnchorByAreaNo(areaNo);
    }

    @Override
    public List<String> checkAreaBoundAnchor() {
        return switchAreaDao.checkAreaBoundAnchor();
    }

    @Override
    public Map<String, Object> getAreaInfo() {
        return this.areaInfoMap;
    }

    @Override
    public void calculateSlotInc(Integer maxAnchor, Integer interval) {
        maxAnchor = Objects.isNull(maxAnchor) || maxAnchor < 6 ? 6 : maxAnchor;
        SysConfig tempConfig = configService.findConfigByCode("max_anchor");
        Integer oldMax = Objects.isNull(tempConfig) ? 6 : Integer.valueOf(tempConfig.getCfgVal());
        if (maxAnchor > oldMax && Objects.nonNull(tempConfig)) {
            tempConfig.setCfgVal(maxAnchor.toString());
            configService.updateConfig(tempConfig);
        }
        Integer slotNumPeriod = null;
        Integer slotSix = null;
        Float locationFerquency;
        Float slotNumRangeTime = null;
        Float slotNumRespTime = null;
        Float slotNum;
        String model = configService.findConfigByCode("slot_inc").getCfgVal();
        switch (model) {
            case "0":
                slotNumPeriod = NumberUtils.toInt(GlobalVariables.SLOT_NUM_0.get("SLOT_NUM_0_PERIOD").toString());
                slotNumRangeTime = Float.valueOf(GlobalVariables.SLOT_NUM_0.get("SLOT_NUM_0_RANGE_PRE6_TIME").toString());
                slotNumRespTime = Float.valueOf(GlobalVariables.SLOT_NUM_0.get("SLOT_NUM_0_RESP_TIME").toString());
                slotSix = NumberUtils.toInt(GlobalVariables.SLOT_NUM_0.get("SLOT_NUM_0_SIX").toString());
                break;
            case "1":
                slotNumPeriod = NumberUtils.toInt(GlobalVariables.SLOT_NUM_10.get("SLOT_NUM_10_PERIOD").toString());
                slotNumRangeTime = Float.parseFloat(GlobalVariables.SLOT_NUM_10.get("SLOT_NUM_10_RANGE_PRE6_TIME").toString());
                slotNumRespTime = Float.valueOf(GlobalVariables.SLOT_NUM_10.get("SLOT_NUM_10_RESP_TIME").toString());
                slotSix = NumberUtils.toInt(GlobalVariables.SLOT_NUM_10.get("SLOT_NUM_10_SIX").toString());
                break;
        }
        if (Objects.nonNull(interval)) {
            locationFerquency = NumberUtils.toFloat(interval.toString());
        } else {
            Map<Integer, Integer> freMap = new HashMap<>();
            this.anchorDao.findAll().forEach(anchor -> {
                if (Objects.nonNull(anchor.getAnchorConfig().getAnchorInterval())) {
                    Integer _interval = anchor.getAnchorConfig().getAnchorInterval();
                    if (freMap.containsKey(_interval)) {
                        freMap.put(_interval, freMap.get(_interval) + 1);
                    } else {
                        freMap.put(_interval, 1);
                    }
                }
            });
            locationFerquency = Collections.max(freMap.values()).floatValue();
        }
        slotNum = slotNumPeriod / (slotNumRangeTime + (maxAnchor - slotSix) * slotNumRespTime);
        tempConfig = configService.findConfigByCode("timesolt_number");
        Integer newVal = (int) Math.floor(slotNum / locationFerquency);
        tempConfig.setCfgVal(newVal.toString());
        configService.updateConfig(tempConfig);
    }

    @Override
    public Integer getMaxAnchor(String areaIds) {
        Integer maxAnchor = null;
        List<Long> ids = areaIds.isEmpty() ? null : Arrays.asList(areaIds.split(",")).stream().map(s -> Long.parseLong(s.trim())).collect(Collectors.toList());
        Iterable<BusAnchorSwitchArea> areaIter = this.getAllArea(ids);
        List<Integer> areaAnchorNumList = new ArrayList<>();
        for (BusAnchorSwitchArea area : areaIter) {
            Set<BusAnchor> anchors = area.getAnchors();
            areaAnchorNumList.add(anchors.size());
        }
        if (!areaAnchorNumList.isEmpty()) {
            Collections.sort(areaAnchorNumList);
            int index = (areaAnchorNumList.size() - 1);
            maxAnchor = areaAnchorNumList.get(index);
        }
        return maxAnchor;
    }

    @Override
    public List<Map<String, Object>> getAllAreas(Long mapId, boolean isNull) {
        Iterable<BusAnchorSwitchArea> allBusArea = null;
        if (isNull) {
            allBusArea = switchAreaDao.findAll();
        } else {
            allBusArea = this.getAreaByMapId(mapId, false);
        }
        List<Map<String, Object>> result = new ArrayList<>();
        for (Iterator i = allBusArea.iterator(); i.hasNext(); ) {
            BusAnchorSwitchArea busArea = (BusAnchorSwitchArea) i.next();
            Map<String, Object> areaMap = new HashMap<>();
            areaMap.put("id", busArea.getId());
            areaMap.put("areaId", busArea.getAreaId());
            areaMap.put("showColor", busArea.getShowColor());
            Set<BusSwitchLine> lines = busArea.getLines();
            Set<BusSwitchLine> tempLines = new HashSet<>();
            for (BusSwitchLine line : lines) {
                if (line.getLineType() == 1) {
                    tempLines.add(line);
                }
            }
            areaMap.put("lines", tempLines);
            result.add(areaMap);
        }
        return result;
    }

    @Override
    public Map<String, Object> areaGroup() {
        Map<String, Object> result = new HashMap<>();
        Specification<BusAnchorSwitchArea> area = (Root<BusAnchorSwitchArea> root, CriteriaQuery<?> query, CriteriaBuilder criteriaBuilder) -> {
            Predicate queryPre = criteriaBuilder.equal(root.get("mapId"), criteriaBuilder.nullLiteral(Long.class));
            return queryPre;
        };
        Long count = switchAreaDao.count(area);
        Long areaNum = switchAreaDao.count();
        if (count > 0) {
            Iterator<BusMap> mapIter = mapService.getAllMap().iterator();
            while (mapIter.hasNext()) {
                BusMap map = mapIter.next();
                result.put(map.getId().toString(), areaNum);
            }
            result.put("isNull", true);
        } else {
            List<Map<String, Object>> list = switchAreaDao.areaGroup();
            for (Map<String, Object> ml : list) {
                result.put(ml.get("mapId").toString(), ml.get("count"));
            }
            result.put("isNull", false);
        }
        return result;
    }

    @Override
    public Integer delAreaByMapId(Long mapId) {
        List<BusAnchorSwitchArea> allBusArea = this.getAreaByMapId(mapId, true);
        List<Long> ids = new ArrayList<>();
        for (BusAnchorSwitchArea area : allBusArea) {
            ids.add(area.getId());
        }
        return delArea(ids);
    }

    public List<BusAnchorSwitchArea> getAreaByMapId(Long mapId, Boolean isDel) {
        Iterable<BusAnchorSwitchArea> allBusArea = switchAreaDao.findAll((root, criteriaQuery, criteriaBuilder) -> {
            Predicate pred1 = criteriaBuilder.equal(root.get("mapId"), mapId);
            Predicate pred2 = criteriaBuilder.ge(root.get("mapId"), 0);
            if (isDel) {
                return pred1;
            } else {
                return criteriaBuilder.or(pred1, pred2);
            }
        });
        List<BusAnchorSwitchArea> areaList = new ArrayList<>();
        allBusArea.forEach(area -> areaList.add(area));
        return areaList;
    }

    public Set<BusSwitchLine> formatLine(BusAnchorSwitchArea switchArea, String pointsStr) {
        Long startX = Long.MAX_VALUE, startY = Long.MAX_VALUE;
        Long endX = Long.MIN_VALUE, endY = Long.MIN_VALUE;
        if (StringUtils.isNoneBlank(pointsStr)) {
            Set<BusSwitchLine> lines = new HashSet<BusSwitchLine>();
            String[] pointsArr = pointsStr.split(" ");
            int index = 1;
            Integer length = null;
            switch (switchArea.getGraphicsType()) {
                case 1:
                    length = pointsArr.length;
                    break;
                case 2:
                    length = pointsArr.length - 1;
                    break;
            }
            for (int i = 0; i < length; i++) {
                String start = pointsArr[i];
                String end = null;
                if ((i + 1) <= (length - 1)) {
                    end = pointsArr[(i + 1)];
                } else {
                    end = pointsArr[0];
                }
                BusSwitchLine tempLine = new BusSwitchLine();
                tempLine.setLineType(1);
                tempLine.setPolyIndex(index);
                tempLine.setArea(switchArea);
                String[] pStartArr = start.split(",");
                tempLine.setSx(NumberUtils.toLong(pStartArr[0]));
                tempLine.setSy(NumberUtils.toLong(pStartArr[1]));
                startX = startX < NumberUtils.toLong(pStartArr[0]) ? startX : NumberUtils.toLong(pStartArr[0]);
                startY = startY < NumberUtils.toLong(pStartArr[1]) ? startY : NumberUtils.toLong(pStartArr[1]);
                String[] pEndArr = end.split(",");
                tempLine.setTx(NumberUtils.toLong(pEndArr[0]));
                tempLine.setTy(NumberUtils.toLong(pEndArr[1]));
                endX = endX > NumberUtils.toLong(pEndArr[0]) ? endX : NumberUtils.toLong(pEndArr[0]);
                endY = endY > NumberUtils.toLong(pEndArr[1]) ? endY : NumberUtils.toLong(pEndArr[1]);
                lines.add(tempLine);
                index++;
            }
            switchArea.setStartX(startX);
            switchArea.setStartY(startY);
            switchArea.setEndX(endX);
            switchArea.setEndY(endY);
            return lines;
        }
        return null;
    }

    @Override
    public Integer getAreaGraphicsType() {
        return this.GraphicsType;
    }

    @Override
    @Transactional(rollbackOn = Exception.class)
    public void mapLink(BusAnchorSwitchArea switchArea) {
        switchAreaDao.save(switchArea);
    }

    @Override
    public List<Object[]> getAreasByAnchorId(String ids) {
        List<Long> idList = this.idTypeChange(ids);
        List<Long> temp = null;
        List<Object[]> regionObjArr = new ArrayList<>();
        List<Object[]> objTemp = null;
        AnchorVersionVariable anchorVersionVar = (AnchorVersionVariable) SpringUtil.getBean(AnchorVersionVariable.class);
        BusAnchorVersion version = null;
        int regionNum = 5;
        Optional<BusAnchor> anchorOpt = null;
        BusAnchor anchor = null;
        BusAnchorConfig anchorConfig = null;
        for (Long anchorPk : idList) {
            regionNum = anchorVersionVar.getAnchorRegionNum();
            anchorOpt = anchorDao.findById(anchorPk);
            if (anchorOpt.isPresent()) {
                anchor = anchorOpt.get();
                anchorConfig = anchor.getAnchorConfig();
                version = Objects.nonNull(anchorConfig.getVersionCode()) ? anchorVersionVar.getVersionSpecByVersionCode(anchorConfig.getVersionCode()) : null;
                if (!ObjectUtils.isEmpty(version)) {
                    regionNum = version.getRegionNum();
                }
                temp = new ArrayList<>();
                temp.add(anchor.getId());
                objTemp = switchAreaDao.getAreasByAnchorId(temp, regionNum);
                if (!ObjectUtils.isEmpty(objTemp)) {
                    regionObjArr.addAll(objTemp);
                }
            }
        }
        return regionObjArr;
    }

    public List<Long> idTypeChange(String anchorIds) {
        String[] idArr = StringUtils.split(anchorIds, ",");
        List<Long> idList = new ArrayList<Long>();
        for (String idStr : idArr) {
            if (NumberUtils.isCreatable(idStr)) {
                idList.add(NumberUtils.toLong(idStr));
            }
        }
        return idList;
    }

    @Override
    public Map<String, Object> checkSeq() {
        GlobalVariables.rwl.readLock().lock();
        Map<String, Object> resultMap = null;
        try {
            Thread.sleep(1000L);
            resultMap = new HashMap<>();
            List<Object[]> objects = switchAreaDao.checkSeq();
            List<Long> ids = switchAreaDao.checkIsOneAnchorIII();
            for (Object[] object : objects) {
                if (Objects.nonNull(object[1])) {
                    List<String> seqList = Arrays.asList(object[1].toString().split(","));
                    Set<String> seqSet = new HashSet<>(seqList);
                    if (seqList.size() != seqSet.size()) {
                        if (resultMap.containsKey("Repeat")) {
                            String _repeat = resultMap.get("Repeat").toString();
                            _repeat = _repeat.concat("、").concat(object[0].toString());
                            resultMap.put("Repeat", _repeat);
                        } else {
                            resultMap.put("Repeat", object[0].toString());
                        }
                    }
                    if (seqList.contains("0")) {
                        if (resultMap.containsKey("Zero")) {
                            String _zero = resultMap.get("Zero").toString();
                            _zero = _zero.concat("、").concat(object[0].toString());
                            resultMap.put("Zero", _zero);
                        } else {
                            resultMap.put("Zero", object[0].toString());
                        }
                    }
                }
                if (!object[2].equals(object[3])) {
                    if (resultMap.containsKey("Null")) {
                        String _null = resultMap.get("Null").toString();
                        _null = _null.concat("、").concat(object[0].toString());
                        resultMap.put("Null", _null);
                    } else {
                        resultMap.put("Null", object[0].toString());
                    }
                }
                if (Integer.parseInt(object[4].toString()) > 1) {
                    if (resultMap.containsKey("MaxCount")) {
                        String _maxCount = resultMap.get("MaxCount").toString();
                        _maxCount = _maxCount.concat("、").concat(object[0].toString());
                        resultMap.put("MaxCount", _maxCount);
                    } else {
                        resultMap.put("MaxCount", object[0].toString());
                    }
                }
            }
            if (Objects.nonNull(ids) && !ids.isEmpty()) {
                resultMap.put("AnchorIII", StringUtils.join(ids, ","));
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            GlobalVariables.rwl.readLock().unlock();
        }
        return resultMap;
    }

    /**
     * 根据区域下发编号查询区域信息
     *
     * @param areaNo
     * @return
     */

    @Override
    public Optional<BusAnchorSwitchArea> getAreaByAreaNo(Long areaNo) {
        return switchAreaDao.findOne((root, criteriaQuery, criteriaBuilder) -> criteriaBuilder.equal(root.get("areaNo"), areaNo));
    }

/*    @Override
    public List<BusAnchorGroup> get(Long areaId) {

        return null;
    }*/
}

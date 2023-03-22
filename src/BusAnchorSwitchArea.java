import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

import javax.persistence.*;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(value = { "neighbors","anchors","lines","anchorGroups"})
@Entity
@Table(name="bus_anchor_switch_area")
public class BusAnchorSwitchArea implements Serializable{

	/**
	 *
	 */
	private static final long serialVersionUID = -2087575893953231355L;

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY )
	private Long id;

	@Column(name = "area_no")
	private Long areaNo;

	@Column(name = "area_id")
	private String areaId;

	@Column(name = "start_x")
	private Long startX;

	@Column(name = "start_y")
	private Long startY;

	@Column(name = "end_x")
	private Long endX;

	@Column(name = "end_y")
	private Long endY;

	@Column(name = "min_z")
	private Long minZ;

	@Column(name = "max_z")
	private Long maxZ;

	@Column(name = "map_id")
	private Long mapId;

	@Column(name = "sort")
	private Integer sort;

	@Column(name = "type")
	private Integer type;

	@Column(name = "color")
	private String showColor;

	@Column(name = "leave_rssi_enable")
	private Integer leaveRSSIEnable;

	@Column(name = "bs_area_type")
	private Integer bsAreaType;

	@Column(name = "graphics_type")
	private Integer graphicsType;

	@Column(name = "border_type")
	private Integer borderType;

	@Column(name = "disable_warning")
	private Integer disableWarning;

	/**级联保存、更新、删除、刷新;延迟加载*/
	@OneToMany(cascade=CascadeType.ALL,fetch=FetchType.LAZY)
	@JoinColumn(name = "area_no",referencedColumnName = "id",insertable=false,updatable=false)
	private Set<BusSwitchLine> lines = new HashSet<BusSwitchLine>();

	/**级联保存、更新、删除、刷新;延迟加载*/
	@OneToMany(cascade=CascadeType.ALL,fetch=FetchType.LAZY)
	@JoinColumn(name = "area_no",referencedColumnName = "id" ,updatable = false)
	private Set<BusAnchorSwitchAreaNeighbor> neighbors = new HashSet<BusAnchorSwitchAreaNeighbor>();

	@ManyToMany(cascade = {CascadeType.PERSIST,CascadeType.REFRESH,CascadeType.DETACH},fetch = FetchType.EAGER)
	@JoinTable(
			name="bus_swith_area_anchors",
			joinColumns={@JoinColumn(name="area_no", referencedColumnName="id")},
			inverseJoinColumns={@JoinColumn(name="anchor_id", referencedColumnName="id")})
	private Set<BusAnchor> anchors = new HashSet<BusAnchor>();

	@OneToMany(fetch = FetchType.LAZY)
	@JoinTable(name = "bus_switch_group",
			joinColumns = {@JoinColumn(name = "switch_area_id", referencedColumnName = "id")},
			inverseJoinColumns = {@JoinColumn(name="group_id", referencedColumnName="id")})
	private Set<BusAnchorGroup> anchorGroups;

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public Long getStartX() {
		return startX;
	}

	public void setStartX(Long startX) {
		this.startX = startX;
	}

	public Long getStartY() {
		return startY;
	}

	public void setStartY(Long startY) {
		this.startY = startY;
	}

	public Long getEndX() {
		return endX;
	}

	public void setEndX(Long endX) {
		this.endX = endX;
	}

	public Long getEndY() {
		return endY;
	}

	public void setEndY(Long endY) {
		this.endY = endY;
	}

	public Integer getSort() {
		return sort;
	}

	public void setSort(Integer sort) {
		this.sort = sort;
	}

	public Set<BusAnchorSwitchAreaNeighbor> getNeighbors() {
		return neighbors;
	}

	public void setNeighbors(Set<BusAnchorSwitchAreaNeighbor> neighbors) {
		this.neighbors = neighbors;
	}

	public String getAreaId() {
		return areaId;
	}

	public void setAreaId(String areaId) {
		this.areaId = areaId;
	}

	public Set<BusAnchor> getAnchors() {
		return anchors;
	}

	public void setAnchors(Set<BusAnchor> anchors) {
		this.anchors = anchors;
	}

	public String getShowColor() {
		return showColor;
	}

	public void setShowColor(String showColor) {
		this.showColor = showColor;
	}

	public Long getMinZ() {
		return minZ;
	}

	public void setMinZ(Long minZ) {
		this.minZ = minZ;
	}

	public Long getMaxZ() {
		return maxZ;
	}

	public void setMaxZ(Long maxZ) {
		this.maxZ = maxZ;
	}

	public Long getAreaNo() {
		return areaNo;
	}

	public void setAreaNo(Long areaNo) {
		this.areaNo = areaNo;
	}

	public Integer getType() {
		return type;
	}

	public void setType(Integer type) {
		this.type = type;
	}

	public Integer getLeaveRSSIEnable() {
		return leaveRSSIEnable;
	}

	public void setLeaveRSSIEnable(Integer leaveRSSIEnable) {
		this.leaveRSSIEnable = leaveRSSIEnable;
	}

	public Integer getBsAreaType() {
		return bsAreaType;
	}

	public void setBsAreaType(Integer bsAreaType) {
		this.bsAreaType = bsAreaType;
	}

	public Long getMapId() {
		return mapId;
	}

	public void setMapId(Long mapId) {
		this.mapId = mapId;
	}

	public Integer getGraphicsType() {
		return graphicsType;
	}

	public void setGraphicsType(Integer graphicsType) {
		this.graphicsType = graphicsType;
	}

	public Set<BusSwitchLine> getLines() {
		return lines;
	}

	public void setLines(Set<BusSwitchLine> lines) {
		this.lines = lines;
	}

	public Integer getBorderType() {
		return borderType;
	}

	public void setBorderType(Integer borderType) {
		this.borderType = borderType;
	}

	public Integer getDisableWarning() {
		return disableWarning;
	}

	public void setDisableWarning(Integer disableWarning) {
		this.disableWarning = disableWarning;
	}

	public Set<BusAnchorGroup> getAnchorGroups() {
		return anchorGroups;
	}

	public void setAnchorGroups(Set<BusAnchorGroup> anchorGroups) {
		this.anchorGroups = anchorGroups;
	}
}

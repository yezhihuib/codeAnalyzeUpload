import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.OneToOne;
import javax.persistence.Table;
import javax.validation.constraints.NotEmpty;

import com.alibaba.fastjson.annotation.JSONField;

/**
 * @author jobs
 */
@Entity
@Table(name="bus_anchor")
public class BusAnchor implements Serializable{
    
	/**
	 * 
	 */
	private static final long serialVersionUID = -8283435869448564898L;

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY )
	private Long id;
	
	@NotEmpty(message="基站ID不能为空！")
	@Column(name = "anchor_id",  unique = true,nullable = false)
	private String anchorId;
	
	@Column(name = "anchor_x")
	private Long anchorX;
	
	@Column(name = "anchor_y")
	private Long anchorY;
	
	@Column(name = "anchor_z")
	private Long anchorZ;
	
	@Column(name = "anchor_type")
	private Integer anchorType;
	
	@Column(name = "anchor_bno")
	private Integer anchorBno;
	
	@Column(name = "syn_anchor_id")
	private String synAnchorId;
	
	@Column(name = "enabled")
	private Integer enabled;

	@Column(name = "offset")
	private Long offset;
	
	@Column(name = "is_bs")
	private Integer isBs;
	
	@Column(name = "is_floor")
	private Integer isFloor;

	@Column(name = "sense_raw")
	private Integer senseRaw;
	
	@JSONField(serialize = false)
	@OneToOne(targetEntity = BusAnchorConfig.class,fetch=FetchType.LAZY,cascade = CascadeType.REMOVE)
	@JoinColumn(name = "anchor_id",referencedColumnName = "anchor_id",insertable=false,updatable=false)
	private BusAnchorConfig anchorConfig;
	
	@JSONField(serialize = false)
	@ManyToMany
	@JoinTable(
		      name="bus_anchor_region_relation",
		      joinColumns={@JoinColumn(name="anchor_id", referencedColumnName="id")},
		      inverseJoinColumns={@JoinColumn(name="area_id", referencedColumnName="id")})
	private Set<BusArea> areas = new HashSet<BusArea>();

	@JSONField(serialize = false)
	@ManyToMany
	@JoinTable(
		      name="bus_swith_area_anchors",
		      joinColumns={@JoinColumn(name="anchor_id", referencedColumnName="id")},
		      inverseJoinColumns={@JoinColumn(name="area_no", referencedColumnName="id")})
	private Set<BusAnchorSwitchArea> switchAreas = new HashSet<BusAnchorSwitchArea>();

//	
//	@ManyToMany(mappedBy="anchors")
//	private Set<BusMap> maps = new HashSet<BusMap>();

	private String status;

	@Column(name = "map_id")
	private Long mapId;
	
	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getAnchorId() {
		return anchorId;
	}

	public void setAnchorId(String anchorId) {
		this.anchorId = anchorId;
	}

	public Long getAnchorX() {
		return anchorX;
	}

	public void setAnchorX(Long anchorX) {
		this.anchorX = anchorX;
	}

	public Long getAnchorY() {
		return anchorY;
	}

	public void setAnchorY(Long anchorY) {
		this.anchorY = anchorY;
	}

	public Long getAnchorZ() {
		return anchorZ;
	}

	public void setAnchorZ(Long anchorZ) {
		this.anchorZ = anchorZ;
	}

	public Integer getAnchorType() {
		return anchorType;
	}

	public void setAnchorType(Integer anchorType) {
		this.anchorType = anchorType;
	}

	public Integer getAnchorBno() {
		return anchorBno;
	}

	public void setAnchorBno(Integer anchorBno) {
		this.anchorBno = anchorBno;
	}

	public String getSynAnchorId() {
		return synAnchorId;
	}

	public void setSynAnchorId(String synAnchorId) {
		this.synAnchorId = synAnchorId;
	}

	public Long getOffset() {
		return offset;
	}

	public void setOffset(Long offset) {
		this.offset = offset;
	}

	public Integer getEnabled() {
		return enabled;
	}

	public void setEnabled(Integer enabled) {
		this.enabled = enabled;
	}

	public BusAnchorConfig getAnchorConfig() {
		return anchorConfig;
	}

	public void setAnchorConfig(BusAnchorConfig anchorConfig) {
		this.anchorConfig = anchorConfig;
	}

	public Integer getIsBs() {
		return isBs;
	}

	public void setIsBs(Integer isBs) {
		this.isBs = isBs;
	}

	public Integer getIsFloor() {
		return isFloor;
	}

	public void setIsFloor(Integer isFloor) {
		this.isFloor = isFloor;
	}

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}

	public Long getMapId() {
		return mapId;
	}

	public void setMapId(Long mapId) {
		this.mapId = mapId;
	}

	public Set<BusAnchorSwitchArea> getSwitchAreas() {
		return switchAreas;
	}

	public void setSwitchAreas(Set<BusAnchorSwitchArea> switchAreas) {
		this.switchAreas = switchAreas;
	}

	public Integer getSenseRaw() {
		return senseRaw;
	}

	public void setSenseRaw(Integer senseRaw) {
		this.senseRaw = senseRaw;
	}

	public Set<BusArea> getAreas() {
		return areas;
	}

	public void setAreas(Set<BusArea> areas) {
		this.areas = areas;
	}
}

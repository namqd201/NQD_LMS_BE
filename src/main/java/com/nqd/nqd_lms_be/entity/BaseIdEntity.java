package com.nqd.nqd_lms_be.entity;

import jakarta.persistence.MappedSuperclass;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldNameConstants;

import java.io.Serializable;

@MappedSuperclass
@Getter
@Setter
public abstract class BaseIdEntity implements Serializable {
}

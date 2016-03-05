package com.project.nicki.displaystabilizer.dataprocessor.utils.Vect2Mat;
public class Vector3D extends VectorN {

    public Vector3D() { super(4); }                                    // create a new 3D homogeneous vector

    public void set(double x, double y, double z, double w) {          // set value of vector
        set(0, x);
        set(1, y);
        set(2, z);
        set(3, w);
    }
    public void set(double x, double y, double z) { set(x, y, z, 1); } // set value of a 3D point
}

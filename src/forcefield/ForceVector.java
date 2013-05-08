package forcefield;

import com.sun.j3d.utils.geometry.Sphere;
import particles.ParticleSystem;

import javax.media.j3d.*;
import javax.vecmath.Color4f;
import javax.vecmath.Point3f;
import javax.vecmath.Vector3f;
import java.awt.*;

/**
 * A particle. Meant to live within the ParticleSystem class.
 */
public class ForceVector extends BranchGroup {
    private static final float DEFAULT_MASS = 10;

    //these are all required for rendering
    public final Point3f position;
    public final VectorField vectorField;

    //these are for the simulation
    public Vector3f velocity;
    public float mass;

    //workspace variables
    private Vector3f v3f;
    public Vector3f forceAccumulator;
    private final int index;

    public ForceVector(final VectorField vectorField, final int index, final Point3f position) {
        this.index = index;
        this.vectorField = vectorField;
        this.position = position;
        this.velocity = new Vector3f();
        this.mass = DEFAULT_MASS;
        v3f = new Vector3f();


        forceAccumulator = new Vector3f();
        updateTransformGroup();

    }

    public void updateTransformGroup() {
        position.get(v3f);
//        t3d.setTranslation(v3f);
//        tg.setTransform(t3d);
    }

    /**
     * @return the particle's current position
     */
    public Point3f getPosition() {
        return position;
    }

    /**
     * update the particle
     * @param dt The time difference
     */
    public void update(float dt) {
        // The force accumulator vector (net force) now becomes
        // the acceleration vector.
        forceAccumulator.scale(1 / mass);
        position.scaleAdd(dt, velocity, position);
        position.scaleAdd(dt*dt / 2, forceAccumulator, position);
        velocity.scaleAdd(dt, forceAccumulator, velocity);
    }

    private final float[] points = new float[2*3];

    private void setForceVector(final Vector3f forceVector) {
        vectorField.setForce(index,forceVector);
    }
}
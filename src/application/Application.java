package application;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GraphicsConfiguration;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.HashSet;
import java.util.Set;

import javax.media.j3d.AmbientLight;
import javax.media.j3d.Appearance;
import javax.media.j3d.BoundingBox;
import javax.media.j3d.BoundingSphere;
import javax.media.j3d.BranchGroup;
import javax.media.j3d.Canvas3D;
import javax.media.j3d.ColoringAttributes;
import javax.media.j3d.DirectionalLight;
import javax.media.j3d.Light;
import javax.media.j3d.PolygonAttributes;
import javax.media.j3d.Transform3D;
import javax.media.j3d.TransformGroup;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.vecmath.Color3f;
import javax.vecmath.Point3d;
import javax.vecmath.Vector3d;
import javax.vecmath.Vector3f;

import forcefield.VectorField;
import particles.ParticleSystem;

import com.sun.j3d.utils.geometry.Box;
import com.sun.j3d.utils.universe.SimpleUniverse;

import particles.behaviors.*;

public class Application {

	// Physics updates per second (approximate).
	private static final int UPDATE_RATE = 30;
	
	// Width of the extent in meters.
	private static final float EXTENT_WIDTH = 20;
	
	// Current particle behavior.
	private ParticleBehavior currentBehavior;
	
	// Magnitude of the force being applied.
	private int forceMagnitude;
	
	// Coefficient of restitution.
	private float coefficientOfRestitution = 1f;
	
	// Simple universe reference.
	private SimpleUniverse simpleU;
	
	// Particle system
    private ParticleSystem particleSystem;
    private VectorField forceField;
    private static final int PARTICLE_COUNT = 50;
    private static final int FIELD_DIVISIONS = 20;
    double DISTANCE = 15d;

    private Set<ForceBehavior> forceBehaviors = new HashSet<ForceBehavior>();
    private Set<CollisionBehavior> collisionBehaviors = new HashSet<CollisionBehavior>();

    ForceBehavior currentForceBehavior = null;

    /**
     * Main method.
     * 
     * @param args
     */
    public static void main(String[] args) {
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				new Application().createAndShowGUI();
			}
		});
	}

	/**
	 * Create and display the GUI, including menus and the scene graph. 
	 */
	private void createAndShowGUI() {
		// Fix for background flickering on some platforms
		System.setProperty("sun.awt.noerasebackground", "true");
		
		// Build the universe
		GraphicsConfiguration config = SimpleUniverse
				.getPreferredConfiguration();
		final Canvas3D canvas = new Canvas3D(config);

		simpleU = new SimpleUniverse(canvas);
		simpleU.getViewingPlatform().setNominalViewingTransform();
		simpleU.getViewer().getView().setSceneAntialiasingEnable(true);

		// Scene graph
		
		// Add a scaling transform that resizes the virtual world to fit
		// within the standard view frustum.
		BranchGroup trueScene = new BranchGroup();
		TransformGroup worldScaleTG = new TransformGroup();
		Transform3D t3D = new Transform3D();
		t3D.setScale( 6 / EXTENT_WIDTH);
		worldScaleTG.setTransform(t3D);
		trueScene.addChild(worldScaleTG);
		BranchGroup scene = new BranchGroup();
		worldScaleTG.addChild(scene);

		Light light = new AmbientLight(new Color3f(1f, 1f, 1f));
		scene.addChild(light);
		light = new DirectionalLight(new Color3f(1f, 1f, 1f), new Vector3f(-1f, -1f, -1f));
		light.setInfluencingBounds(new BoundingBox());
		
		// View movement
		Point3d focus = new Point3d();
        Point3d camera = new Point3d(0,0,1);
        Vector3d up = new Vector3d(0,1,0);
        TransformGroup lightTransform = new TransformGroup();
        TransformGroup curTransform = new TransformGroup();
        FlyCam fc = new FlyCam(simpleU.getViewingPlatform().getViewPlatformTransform(),focus,camera,up,DISTANCE, lightTransform, curTransform);
        fc.setSchedulingBounds(new BoundingSphere(new Point3d(),1000.0));
        scene.addChild(fc);

		// Extent
        Appearance app = new Appearance();
        PolygonAttributes polyAttribs = new PolygonAttributes(PolygonAttributes.POLYGON_LINE, PolygonAttributes.CULL_NONE, 0);
        app.setPolygonAttributes(polyAttribs);
        app.setColoringAttributes(new ColoringAttributes(new Color3f(1.0f, 1.0f, 1.0f), ColoringAttributes.FASTEST));
        float dim = EXTENT_WIDTH / 2  - 0.02f;
		Box extent = new Box(dim, dim, dim, app);
        extent.setPickable(false);
        scene.addChild(extent);

        //extent solid back
        app = new Appearance();
        polyAttribs = new PolygonAttributes(PolygonAttributes.POLYGON_FILL, PolygonAttributes.CULL_FRONT, 0);
        app.setPolygonAttributes(polyAttribs);
        app.setColoringAttributes(new ColoringAttributes(new Color3f(0.5f, 0.5f, 0.5f), ColoringAttributes.FASTEST));
        dim = EXTENT_WIDTH / 2;
        extent = new Box(dim, dim, dim, app);
		extent.setPickable(false);
		scene.addChild(extent);
        particleSystem = new ParticleSystem(PARTICLE_COUNT,EXTENT_WIDTH/2);
        scene.addChild(particleSystem);
        forceField = new VectorField(EXTENT_WIDTH/2f, FIELD_DIVISIONS);
        scene.addChild(forceField);
		
		simpleU.addBranchGraph(trueScene);
        addBehaviors(particleSystem);

		// Swing-related code
		JFrame appFrame = new JFrame("Physics Engine - Project 2");
		appFrame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        canvas.setPreferredSize(new Dimension(800,600));
		appFrame.add(canvas, BorderLayout.CENTER);
		appFrame.add(buildControlPanel(), BorderLayout.SOUTH);
		appFrame.pack();
        appFrame.setLocationRelativeTo(null); //center the window
        //disabled because it's super annoying for testing --david
//		if (Toolkit.getDefaultToolkit().isFrameStateSupported(
//				Frame.MAXIMIZED_BOTH)) {
//			appFrame.setExtendedState(appFrame.getExtendedState()
//					| Frame.MAXIMIZED_BOTH);
//		}

        //simulation start
        new Timer(1000 / UPDATE_RATE, new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                canvas.stopRenderer();
                tick();
                canvas.startRenderer();
            }
        }).start();
		appFrame.setVisible(true);
	}

    private void addBehaviors(ParticleSystem particleSystem) {
        forceBehaviors.add(new GravityBehavior(10));
        forceBehaviors.add(new WindBehavior(20));
        forceBehaviors.add(new DragBehavior(0.01f));

        collisionBehaviors.add(new CubeBoundingBehavior(EXTENT_WIDTH/2, coefficientOfRestitution));

        for(ForceBehavior fb : forceBehaviors) {
            particleSystem.addParticleForceBehavior(fb);
            forceField.addParticleForceBehavior(fb);
        }

        for(CollisionBehavior cb : collisionBehaviors) {
            particleSystem.addParticleCollisionBehavior(cb);
        }


//        WindBehavior wb = new WindBehavior(20);
//        forceBehaviors.add(wb);
//        forceField.addParticleForceBehavior(wb);
    }


    long old_time = System.currentTimeMillis();
    long new_time;
    private void tick() {
        new_time = System.currentTimeMillis();
        float difference = (new_time - old_time)/ 1000f;
        particleSystem.update(difference);
        forceField.update(difference);
        for(ForceBehavior fb : forceBehaviors) {
            fb.update(difference);
        }
        for(CollisionBehavior cb : collisionBehaviors) {
            cb.update(difference);
        }

        old_time = new_time;
    }
	
	private final JPanel buildControlPanel() {
		// Basic panel setups
		JPanel controlPanel = new JPanel();
		JPanel radioButtonPanel = new JPanel();
		JPanel sliderPanel = new JPanel();
		
		GridLayout radioButtonGrid = new GridLayout(0, 2);
		GridLayout sliderGrid = new GridLayout(0, 3);
		radioButtonPanel.setLayout(radioButtonGrid);
		sliderPanel.setLayout(sliderGrid);
		
		controlPanel.add(radioButtonPanel, BorderLayout.EAST);
		controlPanel.add(sliderPanel, BorderLayout.WEST);

		// Add controls for forces
		JSlider forceMagnitudeSlider = buildSlider(0, 100, 100);
        forceMagnitudeSlider.setMajorTickSpacing(1000);
        forceMagnitudeSlider.setMinorTickSpacing(250);
		JSlider coefficientOfRestitutionSlider = buildSlider(0, 100, (int)(coefficientOfRestitution*100));
		
		// TODO Use the buildRadioButton() method to create these
//		JRadioButton enableFirstForce = new JRadioButton();
//		JRadioButton enableSecondForce = new JRadioButton();
//		JRadioButton enableThirdForce = new JRadioButton();

		ButtonGroup group = new ButtonGroup();
        for(ForceBehavior fb : forceBehaviors) {
            JRadioButton jrb = new JRadioButton();
            jrb.addActionListener(new ActionListener() {
                public ForceBehavior associatedBehavior;
                public JSlider slider;

                @Override
                public void actionPerformed(ActionEvent e) {
                    currentForceBehavior = associatedBehavior;
                    float max = associatedBehavior.getForceMaximum();
                    float min = associatedBehavior.getForceMinimum();
                    float cur = associatedBehavior.getForceMagnitude();
                    slider.setMaximum((int) (max * 100));
                    slider.setMinimum((int) (min * 100));
                    slider.setValue((int) (cur * 100));
                    System.out.printf("Slider Changed: Min (%02.2f) Max (%02.2f) Cur (%02.2f)\n",min,max,cur);
                }

                /*Call immediately after to associate with correct ForceBehavior*/
                public ActionListener init(ForceBehavior associatedBehavior, JSlider slider) {
                    this.associatedBehavior = associatedBehavior;
                    this.slider = slider;
                    this.actionPerformed(null);
                    return this;
                }
            }.init(fb, forceMagnitudeSlider));

            group.add(jrb);
            jrb.setText(fb.getName());
            radioButtonPanel.add(jrb);
        }
        //select the first element
        if(group.getElements().hasMoreElements())
            group.getElements().nextElement().setSelected(true);
//		group.add(enableFirstForce);
//        enableFirstForce.setSelected(true);
//		group.add(enableSecondForce);
//		group.add(enableThirdForce);
//
//		radioButtonPanel.add(enableFirstForce);
//		radioButtonPanel.add(new JLabel("Gravity"));
//
//		radioButtonPanel.add(enableSecondForce);
//		radioButtonPanel.add(new JLabel("Force2"));
//
//		radioButtonPanel.add(enableThirdForce);
//		radioButtonPanel.add(new JLabel("Force3"));
		
		sliderPanel.add(new JLabel("Magnitude"));
		sliderPanel.add(forceMagnitudeSlider);
		final JLabel forceMagLabel = new JLabel("" + forceMagnitudeSlider.getValue());
		sliderPanel.add(forceMagLabel);
		
		sliderPanel.add(new JLabel("Coefficient of restitution"));
		sliderPanel.add(coefficientOfRestitutionSlider);
		final JLabel coefficientLabel = new JLabel("" + coefficientOfRestitutionSlider.getValue());
		sliderPanel.add(coefficientLabel);
		
		ChangeListener forceMagnitudeListener = new ChangeListener() {
			@Override
			public void stateChanged(ChangeEvent e) {
				JSlider source = (JSlider) e.getSource();
				forceMagnitude = source.getValue();
				forceMagLabel.setText("" + Math.round(forceMagnitude)/100f);
                currentForceBehavior.setForceMagnitude(forceMagnitude/100f);
                forceField.resetMaxLength(); //clear the max length so it can adjust quickly
			}
		};
		forceMagnitudeSlider.addChangeListener(forceMagnitudeListener);
		
		ChangeListener coefficientListener = new ChangeListener() {
			@Override
			public void stateChanged(ChangeEvent e) {
				JSlider source = (JSlider) e.getSource();
				coefficientOfRestitution = source.getValue()/100f;
				coefficientLabel.setText("" + source.getValue() + "%");
                //update the coefficients
                for(CollisionBehavior cb : collisionBehaviors)
                    cb.setCoefficientOfRestitution(coefficientOfRestitution);
			}
		};
		coefficientOfRestitutionSlider.addChangeListener(coefficientListener);
		return controlPanel;
	}
	
	private final JSlider buildSlider(int min, int max, int value) {
		JSlider slider = new JSlider(min, max, value);
		slider.setMinorTickSpacing(1);
		slider.setPaintTicks(true);
		return slider;
	}
	
	private final JRadioButton buildRadioButton(final ParticleBehavior changeBehavior) {
		final JRadioButton button = new JRadioButton();
		ActionListener listener = new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				currentBehavior = changeBehavior;
			}
		};
		button.addActionListener(listener);
		return button;
	}
}

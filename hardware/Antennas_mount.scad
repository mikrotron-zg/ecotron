//Antennas mount

ex=0.1;         //utility

mount_thickness=5;
mount_width=12;
mount_length=30;
mount_hole_x=10;
mount_hole_dia=2.5;
mount_hole_head_dia=6;
mount_hole_head_height=3;
GSM_mount_height=60;
GSM_mount_length=20;
GSM_mount_hole_dia=8;
GSM_mount_hole_height=50;
GPS_mount_height=35;
GPS_mount_width=40;
GPS_mount_length=45;
$fn=40;

//start
rotate([90, 0, 0]) antennas_mount();

module antennas_mount(){
    mount_base();
    GSM_mount();
    GPS_mount();
}

module mount_base(){
    difference(){
        cube([mount_length, mount_width, mount_thickness]);
        translate([mount_hole_x, mount_width/2, -ex]) 
            cylinder(h=mount_thickness-mount_hole_head_height+2*ex, d=mount_hole_dia);
        translate([mount_hole_x, mount_width/2, mount_thickness-mount_hole_head_height]) 
            cylinder(h=mount_hole_head_height+ex, d=mount_hole_head_dia);
    }
}

module GSM_mount(){
    difference(){
        cube([mount_thickness, mount_width, GSM_mount_height]);
        translate([-ex, mount_width/2, GSM_mount_hole_height]) 
            rotate([0, 90, 0]) union(){
                cylinder(h=mount_thickness+2*ex, d=GSM_mount_hole_dia);
                cylinder(h=mount_thickness-3+ex, d=GSM_mount_hole_dia+2);
            }
    }
}

module GPS_mount(){
    translate([mount_length-mount_thickness, 0, 0]) 
        cube([mount_thickness, mount_width, GPS_mount_height]);
    translate([mount_length-mount_thickness, 0, GPS_mount_height-mount_thickness]) 
        cube([GPS_mount_length, GPS_mount_width, mount_thickness]);
    translate([mount_length-mount_thickness,GPS_mount_width, GPS_mount_height-mount_thickness])     rotate([180, 0, 0]) 
            prism(mount_thickness, GPS_mount_width-mount_width, GPS_mount_height-mount_thickness);
}

 module prism(l, w, h){
       polyhedron(
               points=[[0,0,0], [l,0,0], [l,w,0], [0,w,0], [0,w,h], [l,w,h]],
               faces=[[0,1,2,3],[5,4,3,2],[0,4,5,1],[0,3,4],[5,2,1]]
               );
 }
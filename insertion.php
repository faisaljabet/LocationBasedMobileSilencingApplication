<?php
    $con = mysqli_connect("localhost", "root", "", "mobilesilencer");

    $latitude = $_POST['latitude'];
    $longitude = $_POST['longitude'];

    $result = mysqli_query($con, "INSERT INTO mosque_info (latitude,longitude) VALUES ('$latitude', '$longitude')");
    if($result)
    {
        $response = "inserted";
    }
    else
    {
        $response = "failed";
    }
    echo $response;
?>